package com.anasteshap.service.impl;

import com.anasteshap.repository.StorageInterface;
import com.anasteshap.service.StorageInterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileStorageInterfaceServiceImpl implements StorageInterfaceService {
    private final StorageInterface storageInterface;

    @Autowired
    public FileStorageInterfaceServiceImpl(StorageInterface storageInterface) {
        this.storageInterface = storageInterface;
    }

    public String handleGetRequest(String key) {
        return storageInterface.get(key);
    }

    public void handleSetRequest(String key, String value) {
        storageInterface.set(key, value);
    }
}

