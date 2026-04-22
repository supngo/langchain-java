package com.naturecode.langchain.tool;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dev.langchain4j.agent.tool.Tool;
import reactor.core.publisher.Mono;

@Component
public class ClaimService {

  private final WebClient webClient;

  public ClaimService() {
    this("http://localhost:8081");
  }

  ClaimService(String baseUrl) {
    this.webClient = WebClient.builder().baseUrl(baseUrl).build();
  }

  @Tool("Get claim status for a claim ID")
  public String getClaimStatusById(String claimId) {

    return webClient.get()
        .uri("/claims/{id}", claimId)
        .retrieve()
        .bodyToMono(String.class)
        .timeout(Duration.ofSeconds(2)) // ✅ prevent hanging
        .retry(1) // ✅ basic resilience
        .onErrorResume(e -> Mono.just("Error: Unable to fetch claim " + claimId))
        .block(); // ⚠️ required
  }

  @Tool("Get claim status for a customer ID")
  public String getClaimStatusByCustomer(String customerId) {

    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/claims")
            .queryParam("customerId", customerId)
            .build())
        .retrieve()
        .bodyToMono(String.class)
        .timeout(Duration.ofSeconds(2))
        .retry(1)
        .onErrorResume(e -> Mono.just("Error: Unable to fetch claims for " + customerId))
        .block();
  }
}