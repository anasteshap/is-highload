package com.anasteshap.repository.impl;

import com.anasteshap.repository.StorageInterface;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.io.Console;

//@Primary
//@Repository
public class RedisStorageInterfaceImpl implements StorageInterface {
    private static final String KEY = "KeyValuePair";
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, String> hashOperations;
//    private HashOperations<String, Object, Object> hashOperations;

    @PostConstruct
    private void init() {
        hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public String get(String key) {
        return hashOperations.get(KEY, key);
    }

    @Override
    public void set(String key, String value) {
        hashOperations.put(KEY, key, value);
    }
}
