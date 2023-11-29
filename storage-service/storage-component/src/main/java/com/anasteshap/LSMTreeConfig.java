package com.anasteshap;

import com.anasteshap.lsm.LSMTree;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class LSMTreeConfig {

    @Bean
    @Scope("singleton")
    public LSMTree lsmTree() {
//        return new LSMTree(10000, 10000, "/custom_db/");
        return new LSMTree(3);
    }
}
