package com.anasteshap;

import com.anasteshap.lms.LSMTree;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LSMTreeConfig {

    @Bean
    public LSMTree lsmTree() {
        return new LSMTree(10000, 10000, "/custom_db/");
    }
}
