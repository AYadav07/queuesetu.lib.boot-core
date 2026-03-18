package com.queuesetu.boot.core.restclient.factory;

import com.queuesetu.boot.core.restclient.client.ApiRestClient;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

@Component
public class RestClientFactory {

    private final RestClient.Builder restClientBuilder;

    public RestClientFactory(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public ApiRestClient connect(String baseUrl) {
        Assert.hasText(baseUrl, "baseUrl must not be blank");
        RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
        return new ApiRestClient(restClient);
    }
}