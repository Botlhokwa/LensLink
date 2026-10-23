package com.example.lenslink.repositories;
public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
