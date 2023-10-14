package com.anasteshap.repository;

public interface StorageInterface {
    String get(String key);
    void set(String key, String value);
}
