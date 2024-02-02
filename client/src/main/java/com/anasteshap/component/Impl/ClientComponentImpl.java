package com.anasteshap.component.Impl;

import com.anasteshap.component.ClientComponent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ClientComponentImpl implements ClientComponent {

    private final WebClient webClient;

    public ClientComponentImpl(WebClient.Builder webClientBuilder) {
//        this.webClient = webClientBuilder.baseUrl("http://storage-service:8181").build();
        this.webClient = webClientBuilder.baseUrl("http://is-highload-storage-service-1:8181").build();
//        this.webClient = webClientBuilder.baseUrl("http://localhost:8181").build();
    }

    public String get(String key) {
        return webClient
                .get()
                .uri("/keys/{key}", key)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public void set(String key, String value) {
        webClient
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/keys/{key}")
                        .queryParam("value", value)
                        .build(key))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
