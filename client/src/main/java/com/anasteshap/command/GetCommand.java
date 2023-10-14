package com.anasteshap.command;

import com.anasteshap.component.ClientComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Service
@Command(name = "get")
public class GetCommand implements Callable<Integer> {
    private final ClientComponent clientComponent;

    @Parameters(index = "0")
    private String key;

    @Autowired
    public GetCommand(ClientComponent clientComponent) {
        this.clientComponent = clientComponent;
    }

    @Override
    public Integer call() {
        String value = clientComponent.get(key);
        System.out.println(key + " : " + value);
        return 0;
    }
}