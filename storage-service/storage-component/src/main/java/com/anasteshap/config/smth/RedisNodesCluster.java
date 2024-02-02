package com.anasteshap.config.smth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties("storage.component.redis-cluster")
public class RedisNodesCluster {
    private List<RedisNodeProperties> redisNodes;
}
