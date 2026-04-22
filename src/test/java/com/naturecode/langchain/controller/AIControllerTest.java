package com.naturecode.langchain.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.naturecode.langchain.dto.AIRequest;
import com.naturecode.langchain.service.AIAssistant;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AIControllerTest {

  private AIAssistant assistant;
  private AIController controller;

  @BeforeEach
  public void setUp() {
    assistant = mock(AIAssistant.class);
    controller = new AIController(assistant);
  }

  @Test
  void ask_returnsAssistantResponse() {
    AIRequest request = new AIRequest();
    request.setUserId("user1");
    request.setQuestion("What is my claim status?");

    when(assistant.ask("user1", "What is my claim status?"))
        .thenReturn(Mono.just("Your claim CLM-001 is APPROVED."));

    StepVerifier.create(controller.ask(request))
        .expectNext("Your claim CLM-001 is APPROVED.")
        .verifyComplete();
  }

  @Test
  void ask_passesUserIdAndQuestionToAssistant() {
    AIRequest request = new AIRequest();
    request.setUserId("user42");
    request.setQuestion("How do I submit a claim?");

    when(assistant.ask("user42", "How do I submit a claim?"))
        .thenReturn(Mono.just("ok"));

    controller.ask(request).block();

    verify(assistant).ask("user42", "How do I submit a claim?");
  }

  @Test
  void ask_propagatesEmptyResponse() {
    AIRequest request = new AIRequest();
    request.setUserId("user1");
    request.setQuestion("anything");

    when(assistant.ask("user1", "anything")).thenReturn(Mono.empty());

    StepVerifier.create(controller.ask(request))
        .verifyComplete();
  }
}
