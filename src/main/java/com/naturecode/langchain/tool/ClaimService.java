package com.naturecode.langchain.tool;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dev.langchain4j.agent.tool.Tool;

@Component
public class ClaimService {

    private final WebClient webClient;

    public ClaimService() {
        this.webClient = WebClient.builder().baseUrl("http://localhost:8081").build();
    }

    @Tool("Get claim status for a claim ID")
    public String getClaimStatusById(String claimId) {
        return webClient.get()
                .uri("/claims/" + claimId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }


    @Tool("Get claim status for a customer ID")
    public String getClaimStatusByCustomer(String customerId) {
        return webClient.get()
                .uri("/claims?customerId=" + customerId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}