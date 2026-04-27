package com.queuesetu.boot.core.restclient.client;

import com.queuesetu.boot.core.restclient.exception.RestClientException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiRestClient {

    private final RestClient restClient;
    private String path;
    private final Map<String, String> headers;
    private final Map<String, Object> pathParams;
    private final Map<String, Object> queryParams;
    private Object body;

    public ApiRestClient(RestClient restClient) {
        this.restClient = restClient;
        this.path = "/";
        this.headers = new LinkedHashMap<>();
        this.pathParams = new LinkedHashMap<>();
        this.queryParams = new LinkedHashMap<>();
        this.body = null;
    }

    public ApiRestClient path(String path) {
        this.path = path;
        return this;
    }

    public ApiRestClient header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public ApiRestClient pathParam(String key, Object value) {
        this.pathParams.put(key, value);
        return this;
    }

    public ApiRestClient queryParam(String key, Object value) {
        this.queryParams.put(key, value);
        return this;
    }

    public ApiRestClient body(Object body) {
        this.body = body;
        return this;
    }

    public <T> T get(Class<T> responseType) {
        return executeForEntity(
                "GET",
                restClient
                        .get()
                        .uri(buildUri())
                        .headers(this::applyHeaders),
                responseType
        ).toEntity();
    }

        public <T> RestResponse<T> get(String path, Class<T> responseType) {
        this.path(path);
        return executeForEntity(
            "GET",
            restClient
                .get()
                .uri(buildUri())
                .headers(this::applyHeaders),
            responseType
        );
        }

    public <T> T post(Class<T> responseType) {
        return executeForEntity(
                "POST",
                body != null
                        ? restClient.post().uri(buildUri()).headers(this::applyHeaders).body(body)
                        : restClient.post().uri(buildUri()).headers(this::applyHeaders).contentLength(0),
                responseType
        ).toEntity();
    }

        public <T> RestResponse<T> post(String path, Class<T> responseType) {
        this.path(path);
        return executeForEntity(
            "POST",
            body != null
                    ? restClient.post().uri(buildUri()).headers(this::applyHeaders).body(body)
                    : restClient.post().uri(buildUri()).headers(this::applyHeaders).contentLength(0),
            responseType
        );
        }

        public <T> RestResponse<T> post(String path, Object requestBody, Class<T> responseType) {
        this.path(path);
        this.body(requestBody);
        return executeForEntity(
            "POST",
            body != null
                    ? restClient.post().uri(buildUri()).headers(this::applyHeaders).body(body)
                    : restClient.post().uri(buildUri()).headers(this::applyHeaders).contentLength(0),
            responseType
        );
        }

    public <T> T put(Class<T> responseType) {
        return executeForEntity(
                "PUT",
                body != null
                        ? restClient.put().uri(buildUri()).headers(this::applyHeaders).body(body)
                        : restClient.put().uri(buildUri()).headers(this::applyHeaders).contentLength(0),
                responseType
        ).toEntity();
    }

        public <T> RestResponse<T> put(String path, Class<T> responseType) {
        this.path(path);
        return executeForEntity(
            "PUT",
            body != null
                    ? restClient.put().uri(buildUri()).headers(this::applyHeaders).body(body)
                    : restClient.put().uri(buildUri()).headers(this::applyHeaders).contentLength(0),
            responseType
        );
        }

        public <T> RestResponse<T> put(String path, Object requestBody, Class<T> responseType) {
        this.path(path);
        this.body(requestBody);
        return executeForEntity(
            "PUT",
            body != null
                    ? restClient.put().uri(buildUri()).headers(this::applyHeaders).body(body)
                    : restClient.put().uri(buildUri()).headers(this::applyHeaders).contentLength(0),
            responseType
        );
        }

    public void delete() {
        executeBodiless(
                "DELETE",
                restClient
                        .delete()
                        .uri(buildUri())
                        .headers(this::applyHeaders)
        );
    }

    public <T> RestResponse<T> delete(String path, Class<T> responseType) {
        this.path(path);
        return executeForEntity(
                "DELETE",
                restClient
                        .delete()
                        .uri(buildUri())
                        .headers(this::applyHeaders),
                responseType
        );
    }

    private URI buildUri() {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(path);
        queryParams.forEach(uriBuilder::queryParam);
        return uriBuilder
                .buildAndExpand(pathParams)
                .encode()
                .toUri();
    }

    private void applyHeaders(HttpHeaders httpHeaders) {
        headers.forEach(httpHeaders::set);
    }

    private <T> RestResponse<T> executeForEntity(String method, RestClient.RequestHeadersSpec<?> requestSpec, Class<T> responseType) {
        try {
            ResponseEntity<T> responseEntity = requestSpec.retrieve().toEntity(responseType);
            return new DefaultRestResponse<>(responseEntity.getBody(), responseEntity.getStatusCode().value());
        } catch (Exception ex) {
            throw new RestClientException(method + " request failed for path: " + path, ex);
        } finally {
            resetState();
        }
    }

    private void executeBodiless(String method, RestClient.RequestHeadersSpec<?> requestSpec) {
        try {
            requestSpec.retrieve().toBodilessEntity();
        } catch (Exception ex) {
            throw new RestClientException(method + " request failed for path: " + path, ex);
        } finally {
            resetState();
        }
    }

    private void resetState() {
        this.path = "/";
        this.headers.clear();
        this.pathParams.clear();
        this.queryParams.clear();
        this.body = null;
    }
}
