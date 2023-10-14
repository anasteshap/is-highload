package com.anasteshap.command;

import com.anasteshap.component.ClientComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Service
@Command(name = "set")
public class SetCommand implements Callable<Integer> {
    private final ClientComponent clientComponent;

    @Parameters(index = "0")
    private String key;

    @Parameters(index = "1")
    private String value;

    @Autowired
    public SetCommand(ClientComponent clientComponent) {
        this.clientComponent = clientComponent;
    }

    @Override
    public Integer call() {
        clientComponent.set(key, value);
        System.out.println("Save successfully - " + key + " : " + value);
        return 0;
    }
}
