package com.naturecode.langchain.memory;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

@Component
public class MemoryStore {

  private final ConcurrentHashMap<String, ChatMemory> memoryMap = new ConcurrentHashMap<>();

  public ChatMemory getMemory(String userId) {
    return memoryMap.computeIfAbsent(userId,
        id -> MessageWindowChatMemory.withMaxMessages(20));
  }
}
