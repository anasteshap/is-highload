package com.anasteshap.controller;

import com.anasteshap.service.StorageInterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/keys")
public class ServerComponentController {

    private final StorageInterfaceService serverComponent;

    @Autowired
    public ServerComponentController(StorageInterfaceService serverComponent) {
        this.serverComponent = serverComponent;
    }

    @GetMapping("{key}")
    public ResponseEntity<String> get(@PathVariable String key) {
        return ResponseEntity.ok().body(serverComponent.handleGetRequest(key));
    }

    @PutMapping("{key}")
    public ResponseEntity<?> set(@PathVariable String key, @RequestParam String value) {
        serverComponent.handleSetRequest(key, value);
        return ResponseEntity.ok().build();
    }
}