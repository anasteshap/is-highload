package com.anasteshap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
public class MyCommandFactory implements CommandLine.IFactory {
    private final ApplicationContext applicationContext;

//    @Autowired
    public MyCommandFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <K> K create(Class<K> cls) {
        return applicationContext.getBean(cls);
    }
}
