package com.anasteshap.service;

public interface StorageInterfaceService {
    String handleGetRequest(String key);
    void handleSetRequest(String key, String value);
}
