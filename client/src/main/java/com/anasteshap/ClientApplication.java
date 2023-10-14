package com.anasteshap;

import com.anasteshap.command.AppCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {
    private final CommandLine.IFactory factory;

    @Autowired
    public ClientApplication(AppCommand appCommand, CommandLine.IFactory factory) {
        this.factory = factory;
    }

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        var code = new CommandLine(new AppCommand(), factory).execute(args);
        System.exit(code);
    }
}