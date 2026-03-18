package com.queuesetu.boot.core.restclient.client;

public interface RestResponse<T> {

    T toEntity();

    int getStatus();
}