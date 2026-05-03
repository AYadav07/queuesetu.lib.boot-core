# RestClientFactory — Thread-Safety Design

## Overview

`RestClientFactory` is the single entry-point for creating HTTP clients that
talk to downstream microservices. It wraps Spring's `RestClient` and exposes
a fluent `ApiRestClient` builder per call.

This document explains **why the naïve approach is broken under concurrency**
and **how the current design eliminates that problem**.

---

## The Problem: Shared Mutable Builder

Spring's auto-configured `RestClient.Builder` is a **prototype** bean — it is
meant to be customised and immediately used, not stored as a long-lived field.

### What goes wrong when the builder is kept as a field

```java
// ❌ BROKEN — do NOT do this
@Component
public class RestClientFactory {

    private final RestClient.Builder restClientBuilder;  // shared mutable state!

    public RestClientFactory(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;  // stored as singleton field
    }

    public ApiRestClient connect(String baseUrl) {
        // builder.baseUrl() mutates the same object every caller shares
        RestClient client = restClientBuilder.baseUrl(baseUrl).build();
        return new ApiRestClient(client);
    }
}
```

When two threads call `connect()` at the same time, the following race occurs:

```
Thread A:  restClientBuilder.baseUrl("http://account-service:8086")
                                             ↑ writes field on shared object

Thread B:  restClientBuilder.baseUrl("http://booking-service:8089")
                                             ↑ OVERWRITES field on same object

Thread A:  restClientBuilder.build()
           → builds with "http://booking-service:8089"  ← WRONG URL
```

### Real-world symptom observed in this project

The BFF's branch-detail page fires two requests simultaneously:

| Request                             | Intended service | Port |
| ----------------------------------- | ---------------- | ---- |
| `GET /api/branches/{uuid}`          | account-service  | 8086 |
| `GET /api/services?branchId={uuid}` | booking-service  | 8089 |

Due to the race, the account-service call ended up routed to the booking
service, which has no `/api/branch/*` handler and threw:

```
NoResourceFoundException: No static resource api/branch/e0e6ce3c-...
```

The booking service's `GlobalExceptionHandler` logged this as an `[BOOKING]`
error, making it appear to be a booking-side problem when the real bug was
in the shared builder.

---

## The Fix: Immutable Base Client + `mutate()`

```java
// ✅ CORRECT — current implementation
@Component
public class RestClientFactory {

    private final RestClient baseRestClient;   // immutable, shared safely

    public RestClientFactory(RestClient.Builder restClientBuilder) {
        // Build ONCE at startup. The builder is used here and then discarded.
        // All global settings (codecs, interceptors, default headers) are
        // captured in the immutable baseRestClient.
        this.baseRestClient = restClientBuilder.build();
    }

    public ApiRestClient connect(String baseUrl) {
        // mutate() returns a brand-new Builder copy per invocation.
        // Setting baseUrl on that copy is invisible to every other thread.
        RestClient restClient = baseRestClient.mutate().baseUrl(baseUrl).build();
        return new ApiRestClient(restClient);
    }
}
```

### Thread-safety walkthrough

```
                 ┌─────────────────────────────────────────┐
                 │        baseRestClient (immutable)        │
                 │  codecs / interceptors / default headers │
                 └────────────────┬────────────────────────┘
                                  │  .mutate() copies state
                    ┌─────────────┴──────────────┐
                    ↓                            ↓
          Builder copy A                Builder copy B
    .baseUrl("account:8086")      .baseUrl("booking:8089")
          .build()                        .build()
             │                               │
     RestClient A                    RestClient B
   (account service)               (booking service)
```

Each `connect()` call owns its own independent `Builder` copy. No shared
mutable state exists between threads at any point.

---

## Key API contract

| Method                                  | Thread-safe?   | Notes                                        |
| --------------------------------------- | -------------- | -------------------------------------------- |
| `connect(String baseUrl)`               | ✅ Yes         | Creates an independent `RestClient` per call |
| `ApiRestClient` returned by `connect()` | ⚠️ Per-request | Not shared — create a new one per request    |

> **Important:** `ApiRestClient` holds per-request mutable state (path, headers,
> query params, body). Do **not** store or share an `ApiRestClient` instance
> across multiple threads or multiple requests. Always call `connect()` to get
> a fresh one.

---

## Relevant Spring Documentation

- [`RestClient.mutate()`](<https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestClient.html#mutate()>)  
  Returns a mutable builder initialised with this client's configuration.
- [RestClient — Spring Framework reference](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)
