package com.example.lenslink.repository;

public interface RepositoryCallback<T> {
    void onSuccess(T result);
    /** @param message a user-friendly message safe to show in the UI */
    void onError(String message);
}