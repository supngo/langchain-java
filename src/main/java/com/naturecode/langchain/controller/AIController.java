package com.naturecode.langchain.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naturecode.langchain.dto.AIRequest;
import com.naturecode.langchain.service.AIAssistant;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AIController {

  private final AIAssistant assistant;

  public AIController(AIAssistant assistant) {
    this.assistant = assistant;
  }

  @PostMapping("/ask")
  public Mono<String> ask(@RequestBody AIRequest request) {
    log.info("POST /ai/ask user={} questionLength={}", request.getUserId(), request.getQuestion().length());
    return assistant.ask(request.getUserId(), request.getQuestion())
        .doOnSuccess(r -> log.debug("Response ready user={} responseLength={}", request.getUserId(), r != null ? r.length() : 0));
  }
}
