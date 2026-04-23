package com.naturecode.langchain.service;

import org.springframework.stereotype.Service;

import com.naturecode.langchain.agent.Assistant;
import com.naturecode.langchain.memory.MemoryStore;
import com.naturecode.langchain.tool.ClaimService;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class AIAssistant {

  private final ChatModel model;
  private final ContentRetriever retriever;
  private final ClaimService claimTool;
  private final MemoryStore memoryStore;

  public AIAssistant(ChatModel model,
      ContentRetriever retriever,
      ClaimService claimTool,
      MemoryStore memoryStore) {

    this.model = model;
    this.retriever = retriever;
    this.claimTool = claimTool;
    this.memoryStore = memoryStore;
  }

  public Mono<String> ask(String userId, String question) {
    return Mono.fromCallable(() -> {
      log.debug("Building assistant for user={}", userId);

      Assistant assistant = AiServices.builder(Assistant.class)
          .chatModel(model)
          .contentRetriever(retriever)
          .tools(claimTool)
          .chatMemory(memoryStore.getMemory(userId))
          .build();

      String response = assistant.chat(question);
      log.debug("LLM call complete user={}", userId);
      return response;

    }).subscribeOn(Schedulers.boundedElastic()); // ✅ offload blocking work
  }
}
