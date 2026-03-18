package com.queuesetu.boot.core.restclient.client;

public class DefaultRestResponse<T> implements RestResponse<T> {

    private final T entity;
    private final int status;

    public DefaultRestResponse(T entity, int status) {
        this.entity = entity;
        this.status = status;
    }

    @Override
    public T toEntity() {
        return entity;
    }

    @Override
    public int getStatus() {
        return status;
    }
}