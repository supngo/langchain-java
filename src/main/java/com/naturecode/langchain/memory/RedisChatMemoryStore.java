package com.naturecode.langchain.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

  private static final String KEY_PREFIX = "chat:memory:";
  private static final TypeReference<List<Map<String, String>>> LIST_MAP_TYPE = new TypeReference<>() {};

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Autowired
  public RedisChatMemoryStore(StringRedisTemplate redisTemplate) {
    this(redisTemplate, new ObjectMapper());
  }

  RedisChatMemoryStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<ChatMessage> getMessages(Object memoryId) {
    String json = redisTemplate.opsForValue().get(KEY_PREFIX + memoryId);
    if (json == null) return new ArrayList<>();
    try {
      return objectMapper.readValue(json, LIST_MAP_TYPE).stream()
          .map(this::fromRecord)
          .toList();
    } catch (JsonProcessingException | IllegalArgumentException e) {
      log.warn("Failed to deserialize chat messages for memoryId={}, returning empty list", memoryId, e);
      return new ArrayList<>();
    }
  }

  @Override
  public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    try {
      List<Map<String, String>> records = messages.stream().map(this::toRecord).toList();
      redisTemplate.opsForValue().set(KEY_PREFIX + memoryId, objectMapper.writeValueAsString(records));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize chat messages", e);
    }
  }

  @Override
  public void deleteMessages(Object memoryId) {
    redisTemplate.delete(KEY_PREFIX + memoryId);
  }

  private Map<String, String> toRecord(ChatMessage msg) {
    return switch (msg) {
      case UserMessage m -> Map.of("type", "USER", "text", m.singleText());
      case AiMessage m -> {
        Map<String, String> record = new HashMap<>();
        record.put("type", "AI");
        record.put("text", m.text() != null ? m.text() : "");
        if (m.hasToolExecutionRequests()) {
          try {
            record.put("toolRequests", objectMapper.writeValueAsString(
                m.toolExecutionRequests().stream()
                    .map(r -> Map.of("id", r.id(), "name", r.name(), "arguments", r.arguments()))
                    .toList()));
          } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize tool requests", e);
          }
        }
        yield record;
      }
      case SystemMessage m -> Map.of("type", "SYSTEM", "text", m.text());
      case ToolExecutionResultMessage m ->
          Map.of("type", "TOOL_RESULT", "id", m.id(), "toolName", m.toolName(), "text", m.text());
      default -> throw new IllegalArgumentException("Unsupported message type: " + msg.getClass());
    };
  }

  private ChatMessage fromRecord(Map<String, String> r) {
    return switch (r.get("type")) {
      case "USER"   -> UserMessage.from(r.get("text"));
      case "AI"     -> {
        String toolRequestsJson = r.get("toolRequests");
        if (toolRequestsJson != null) {
          try {
            List<ToolExecutionRequest> reqs = objectMapper.readValue(toolRequestsJson, LIST_MAP_TYPE)
                .stream()
                .map(t -> ToolExecutionRequest.builder()
                    .id(t.get("id"))
                    .name(t.get("name"))
                    .arguments(t.get("arguments"))
                    .build())
                .toList();
            String text = r.get("text");
            yield (text != null && !text.isEmpty())
                ? AiMessage.from(text, reqs)
                : AiMessage.from(reqs);
          } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize tool requests", e);
          }
        }
        yield AiMessage.from(r.get("text"));
      }
      case "SYSTEM"      -> SystemMessage.from(r.get("text"));
      case "TOOL_RESULT" -> ToolExecutionResultMessage.from(r.get("id"), r.get("toolName"), r.get("text"));
      default -> throw new IllegalArgumentException("Unknown message type: " + r.get("type"));
    };
  }
}
