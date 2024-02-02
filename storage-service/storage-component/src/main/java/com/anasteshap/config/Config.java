package com.anasteshap.config;

import com.anasteshap.repository.StorageInterface;
import com.anasteshap.repository.impl.LSMTStorageInterfaceImpl;
import com.anasteshap.repository.impl.RedisStorageInterfaceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    @Bean
    @ConditionalOnProperty(name = "storage-component", havingValue = "lsm")
    public StorageInterface getRepositoryLsmCondition() {
        System.out.println("lsm");
        return new LSMTStorageInterfaceImpl();
    }

    @Bean
    @ConditionalOnProperty(name = "storage-component", havingValue = "redis")
    public StorageInterface getRepositoryRedisCondition() {
        System.out.println("redis");
        return new RedisStorageInterfaceImpl();
    }
}
