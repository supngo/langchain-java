package com.naturecode.langchain.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naturecode.langchain.dto.AIRequest;
import com.naturecode.langchain.service.AIAssistant;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai")
public class AIController {

  private final AIAssistant assistant;

  public AIController(AIAssistant assistant) {
    this.assistant = assistant;
  }

  @PostMapping("/ask")
  public Mono<String> ask(@RequestBody AIRequest request) {
    return assistant.ask(request.getUserId(), request.getQuestion());
  }
}
