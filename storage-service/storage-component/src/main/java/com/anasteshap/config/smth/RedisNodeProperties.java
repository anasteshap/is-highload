package com.anasteshap.config.smth;

import org.springframework.boot.context.properties.ConfigurationProperties;

//@ConfigurationProperties("storage-component.redis-cluster")
public class RedisNodeProperties {
    private String host;
    private String port;
    private String password;
}
