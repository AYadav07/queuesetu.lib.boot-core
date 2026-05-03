package com.queuesetu.boot.core.restclient.factory;

import com.queuesetu.boot.core.restclient.client.ApiRestClient;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

/**
 * Thread-safe factory for creating {@link ApiRestClient} instances scoped to a
 * specific downstream service base URL.
 *
 * <h2>Why this class exists</h2>
 * <p>Spring's auto-configured {@link RestClient.Builder} is a <em>prototype</em>
 * bean — it is designed to be customised and then discarded, not kept as a
 * long-lived field. When a singleton Spring bean (like a service class) injects
 * and stores a {@code RestClient.Builder}, it inadvertently converts that
 * prototype into an effective singleton. Every call to
 * {@code builder.baseUrl(url)} then mutates the <strong>same shared object</strong>.
 *
 * <h2>The concurrency bug this design prevents</h2>
 * <p>Imagine two threads hitting the BFF simultaneously:
 * <pre>
 *   Thread A  →  factory.connect("http://account-service:8086")
 *   Thread B  →  factory.connect("http://booking-service:8089")
 * </pre>
 * If both threads called {@code sharedBuilder.baseUrl(...).build()} on the same
 * builder instance the race would look like:
 * <pre>
 *   Thread A:  sharedBuilder.baseUrl("http://account-service:8086")   // sets field
 *   Thread B:  sharedBuilder.baseUrl("http://booking-service:8089")   // OVERWRITES field
 *   Thread A:  sharedBuilder.build()  // builds with booking URL  ← WRONG
 *   Thread B:  sharedBuilder.build()  // builds with booking URL
 * </pre>
 * Thread A ends up talking to the booking service when it expected to talk to
 * the account service, producing baffling errors like
 * {@code NoResourceFoundException: No static resource api/branch/{uuid}}.
 *
 * <h2>How thread safety is achieved here</h2>
 * <ol>
 *   <li><strong>Build once at startup.</strong> The constructor calls
 *       {@code restClientBuilder.build()} exactly once, capturing all global
 *       settings (message converters, interceptors, codecs) into an immutable
 *       {@link RestClient} stored in {@code baseRestClient}. The builder itself
 *       is then discarded — no reference to it is kept.</li>
 *   <li><strong>Derive per-call clients via {@code mutate()}.</strong>
 *       {@link RestClient#mutate()} returns a <em>brand-new</em>
 *       {@code RestClient.Builder} pre-populated with the base configuration.
 *       Each {@link #connect(String)} invocation creates its own independent
 *       builder, sets its own {@code baseUrl}, and builds its own
 *       {@code RestClient} — with zero shared mutable state between threads.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Inject RestClientFactory wherever you need to call a downstream service.
 * Branch branch = restClientFactory
 *         .connect(accountServiceBaseUrl)   // thread-safe; returns a fresh ApiRestClient
 *         .header("Authorization", authHeader)
 *         .get("/api/branch/" + branchId, Branch.class)
 *         .toEntity();
 * }</pre>
 *
 * @see ApiRestClient
 * @see RestClient#mutate()
 */
@Component
public class RestClientFactory {

    /**
     * Immutable base client holding shared configuration (message converters,
     * interceptors, default headers). Never mutated after construction, making
     * it safe to read from any number of threads simultaneously.
     */
    private final RestClient baseRestClient;

    /**
     * Constructs the factory by eagerly building a single base {@link RestClient}
     * from Spring's auto-configured {@code RestClient.Builder}.
     *
     * <p>The builder is used <em>only here</em> and then dropped. Retaining the
     * builder as a field would re-introduce the thread-safety problem this class
     * is designed to prevent.
     *
     * @param restClientBuilder Spring's prototype {@code RestClient.Builder},
     *                          pre-configured with global codec and interceptor settings.
     */
    public RestClientFactory(RestClient.Builder restClientBuilder) {
        this.baseRestClient = restClientBuilder.build();
    }

    /**
     * Returns a new {@link ApiRestClient} bound to the given {@code baseUrl}.
     *
     * <p><strong>Thread safety:</strong> every invocation calls
     * {@link RestClient#mutate()} on the immutable {@code baseRestClient}, which
     * creates a fresh, independent {@code RestClient.Builder} copy. Setting
     * {@code baseUrl} on that copy does not affect any other thread's copy or
     * the shared {@code baseRestClient}. Multiple threads may therefore call
     * this method concurrently without any synchronisation.
     *
     * @param baseUrl the root URL of the downstream service
     *                (e.g. {@code "http://localhost:8086"})
     * @return a fully configured {@link ApiRestClient} scoped to {@code baseUrl}
     * @throws IllegalArgumentException if {@code baseUrl} is blank or null
     */
    public ApiRestClient connect(String baseUrl) {
        Assert.hasText(baseUrl, "baseUrl must not be blank");
        RestClient restClient = baseRestClient.mutate().baseUrl(baseUrl).build();
        return new ApiRestClient(restClient);
    }
}