package com.anasteshap.command;

import com.anasteshap.component.ClientComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Service
@Command(name = "app", subcommands = {GetCommand.class, SetCommand.class})
public class AppCommand implements Callable<Integer> {
    private static final Logger log = LoggerFactory.getLogger(AppCommand.class);

    @Override
    public Integer call() {
        log.info("Nothing");
        return 0;
    }

//    @Command(name = "get", description = "get command")
//    public Integer GetCommand(String key) {
//        String value = clientComponent.get(key);
//        System.out.println(key + " : " + value);
//        return 0;
//    }
//
//    @Command(name = "set", description = "set command")
//    public Integer SetCommand(String key, String value) {
//        clientComponent.set(key, value);
//        System.out.println("Save successfully - " + key + " : " + value);
//        return 0;
//    }
}