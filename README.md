# Queue Setu REST Client Library

Reusable fluent REST client wrapper built on Spring `RestClient`.

## Package

Base package: `com.queuesetu.restclient`

## Add as dependency in consuming service

If publishing from this repo:

```bash
./gradlew publishToMavenLocal
```

Then in the consuming service `build.gradle`:

```groovy
dependencies {
    implementation 'com.queuesetu:boot-core:0.0.1-SNAPSHOT'
}
```

## Spring wiring in consuming service

Because this library provides `RestClientConfig` and `RestClientFactory`, import them in your app config:

```java
package com.example.config;

import com.queuesetu.restclient.config.RestClientConfig;
import com.queuesetu.restclient.factory.RestClientFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RestClientConfig.class, RestClientFactory.class})
public class ExternalClientLibraryConfig {
}
```

## End-to-end usage in service layer

### 1) One-shot style (path + responseType in same method)

These methods return `RestResponse<T>`, so you can access both body and status:

```java
package com.example.integration;

import com.queuesetu.restclient.client.RestResponse;
import com.queuesetu.restclient.factory.RestClientFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerGateway {

    private final RestClientFactory restClientFactory;

    public CustomerDto getCustomer(Long customerId, String token) {
        RestResponse<CustomerDto> response = restClientFactory
                .connect("https://api.partner.com")
                .header("Authorization", "Bearer " + token)
                .pathParam("id", customerId)
                .queryParam("include", "profile")
                .get("/customers/{id}", CustomerDto.class);

        int status = response.getStatus();
        return response.toEntity();
    }

    public CustomerDto createCustomer(CreateCustomerRequest request, String token) {
        RestResponse<CustomerDto> response = restClientFactory
                .connect("https://api.partner.com")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .post("/customers", request, CustomerDto.class);

        return response.toEntity();
    }

    public CustomerDto updateCustomer(Long customerId, UpdateCustomerRequest request, String token) {
        RestResponse<CustomerDto> response = restClientFactory
                .connect("https://api.partner.com")
                .header("Authorization", "Bearer " + token)
                .pathParam("id", customerId)
                .put("/customers/{id}", request, CustomerDto.class);

        return response.toEntity();
    }

    public void deleteCustomer(Long customerId, String token) {
        restClientFactory
                .connect("https://api.partner.com")
                .header("Authorization", "Bearer " + token)
                .pathParam("id", customerId)
                .delete("/customers/{id}", Void.class);
    }
}
```

### 2) Fluent stateful style

This style returns entity directly for `get/post/put` and bodiless response for `delete`.

```java
package com.example.integration;

import com.queuesetu.restclient.client.ApiRestClient;
import com.queuesetu.restclient.factory.RestClientFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerGateway {

    private final RestClientFactory restClientFactory;

    public CustomerDto getCustomer(Long customerId, String token) {
        ApiRestClient client = restClientFactory.connect("https://api.partner.com");

        return client
                .path("/customers/{id}")
                .pathParam("id", customerId)
                .header("Authorization", "Bearer " + token)
                .queryParam("include", "profile")
                .get(CustomerDto.class);
    }

    public CustomerDto createCustomer(CreateCustomerRequest request, String token) {
        ApiRestClient client = restClientFactory.connect("https://api.partner.com");

        return client
                .path("/customers")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(request)
                .post(CustomerDto.class);
    }

    public CustomerDto updateCustomer(Long customerId, UpdateCustomerRequest request, String token) {
        ApiRestClient client = restClientFactory.connect("https://api.partner.com");

        return client
                .path("/customers/{id}")
                .pathParam("id", customerId)
                .header("Authorization", "Bearer " + token)
                .body(request)
                .put(CustomerDto.class);
    }

    public void deleteCustomer(Long customerId, String token) {
        ApiRestClient client = restClientFactory.connect("https://api.partner.com");

        client
                .path("/customers/{id}")
                .pathParam("id", customerId)
                .header("Authorization", "Bearer " + token)
                .delete();
    }
}
```

## Response abstraction

- `RestResponse<T>` provides:
  - `T toEntity()`
  - `int getStatus()`
- Default implementation is `DefaultRestResponse<T>`.

## Important behavior

- `ApiRestClient` supports fluent request building: path, headers, path params, query params, and body.
- `ApiRestClient` supports one-shot overloads: `get(path, type)`, `post(path, body, type)`, `put(path, body, type)`, `delete(path, type)`.
- After each request execution (`get`, `post`, `put`, `delete`), internal state is reset automatically.
- Create a new request chain per API call for clear, predictable behavior.
