package com.naturecode.langchain.memory;

import org.springframework.stereotype.Component;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

// import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemoryStore {

  private final RedisChatMemoryStore chatMemoryStore;

  public MemoryStore(RedisChatMemoryStore chatMemoryStore) {
    this.chatMemoryStore = chatMemoryStore;
  }

  // --- Previous in-memory implementation (not suitable for scaling) ---
  // private final ConcurrentHashMap<String, ChatMemory> memoryMap = new ConcurrentHashMap<>();
  //
  // public ChatMemory getMemory(String userId) {
  //   return memoryMap.computeIfAbsent(userId,
  //       id -> MessageWindowChatMemory.withMaxMessages(20));
  // }

  public ChatMemory getMemory(String userId) {
    return MessageWindowChatMemory.builder()
        .id(userId)
        .maxMessages(20)
        .chatMemoryStore(chatMemoryStore)
        .build();
  }
}
