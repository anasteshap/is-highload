package com.anasteshap.repository.impl;

import com.anasteshap.lms.LSMTree;
import com.anasteshap.repository.StorageInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class LSMTStorageInterfaceImpl implements StorageInterface {
    private final LSMTree lsmTree;

    @Autowired
    public LSMTStorageInterfaceImpl(LSMTree lsmTree) {
        this.lsmTree = lsmTree;
    }

    @Override
    public String get(String key) {
        return lsmTree.get(key);
    }

    @Override
    public void set(String key, String value) {
        lsmTree.set(key, value);
    }
}

