package com.queuesetu.restclient.client;

public interface RestResponse<T> {

    T toEntity();

    int getStatus();
}