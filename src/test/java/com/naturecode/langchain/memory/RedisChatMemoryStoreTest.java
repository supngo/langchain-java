package com.naturecode.langchain.memory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

class RedisChatMemoryStoreTest {

  private ValueOperations<String, String> valueOps;
  private RedisChatMemoryStore store;

  @BeforeEach
  @SuppressWarnings("unchecked")
  public void setUp() {
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    store = new RedisChatMemoryStore(redisTemplate);
  }

  // --- helper: fake Redis backed by a HashMap for round-trip tests ---

  private void useFakeRedis(Map<String, String> fakeStore) {
    doAnswer(inv -> { fakeStore.put(inv.getArgument(0), inv.getArgument(1)); return null; })
        .when(valueOps).set(any(), any());
    when(valueOps.get(any())).thenAnswer(inv -> fakeStore.get(inv.getArgument(0)));
  }

  // --- getMessages ---

  @Test
  void getMessages_returnsEmptyList_whenKeyNotFound() {
    when(valueOps.get("chat:memory:user1")).thenReturn(null);

    assertThat(store.getMessages("user1")).isEmpty();
  }

  @Test
  void getMessages_returnsEmptyList_onCorruptJson() {
    when(valueOps.get("chat:memory:user1")).thenReturn("not valid json {{");

    assertThat(store.getMessages("user1")).isEmpty();
  }

  // --- round-trip tests per message type ---

  @Test
  void roundTrip_userMessage() {
    Map<String, String> fakeStore = new HashMap<>();
    useFakeRedis(fakeStore);

    store.updateMessages("user1", List.of(UserMessage.from("Hello")));
    List<ChatMessage> result = store.getMessages("user1");

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isInstanceOf(UserMessage.class);
    assertThat(((UserMessage) result.get(0)).singleText()).isEqualTo("Hello");
  }

  @Test
  void roundTrip_aiMessage() {
    Map<String, String> fakeStore = new HashMap<>();
    useFakeRedis(fakeStore);

    store.updateMessages("user1", List.of(AiMessage.from("Your claim is approved.")));
    List<ChatMessage> result = store.getMessages("user1");

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isInstanceOf(AiMessage.class);
    assertThat(((AiMessage) result.get(0)).text()).isEqualTo("Your claim is approved.");
  }

  @Test
  void roundTrip_aiMessage_withToolExecutionRequests() {
    Map<String, String> fakeStore = new HashMap<>();
    useFakeRedis(fakeStore);

    ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
        .id("call-1")
        .name("getClaimStatusById")
        .arguments("{\"claimId\":\"CLM-001\"}")
        .build();
    AiMessage aiMessage = AiMessage.from(List.of(toolRequest));

    store.updateMessages("user1", List.of(aiMessage));
    List<ChatMessage> result = store.getMessages("user1");

    assertThat(result).hasSize(1);
    AiMessage restored = (AiMessage) result.get(0);
    assertThat(restored.hasToolExecutionRequests()).isTrue();
    assertThat(restored.toolExecutionRequests().get(0).id()).isEqualTo("call-1");
    assertThat(restored.toolExecutionRequests().get(0).name()).isEqualTo("getClaimStatusById");
    assertThat(restored.toolExecutionRequests().get(0).arguments()).isEqualTo("{\"claimId\":\"CLM-001\"}");
  }

  @Test
  void roundTrip_systemMessage() {
    Map<String, String> fakeStore = new HashMap<>();
    useFakeRedis(fakeStore);

    store.updateMessages("user1", List.of(SystemMessage.from("You are an insurance assistant.")));
    List<ChatMessage> result = store.getMessages("user1");

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isInstanceOf(SystemMessage.class);
    assertThat(((SystemMessage) result.get(0)).text()).isEqualTo("You are an insurance assistant.");
  }

  @Test
  void roundTrip_toolExecutionResultMessage() {
    Map<String, String> fakeStore = new HashMap<>();
    useFakeRedis(fakeStore);

    ToolExecutionResultMessage toolResult =
        ToolExecutionResultMessage.from("call-1", "getClaimStatusById", "APPROVED");

    store.updateMessages("user1", List.of(toolResult));
    List<ChatMessage> result = store.getMessages("user1");

    assertThat(result).hasSize(1);
    ToolExecutionResultMessage restored = (ToolExecutionResultMessage) result.get(0);
    assertThat(restored.id()).isEqualTo("call-1");
    assertThat(restored.toolName()).isEqualTo("getClaimStatusById");
    assertThat(restored.text()).isEqualTo("APPROVED");
  }

  @Test
  void roundTrip_multipleMessages_preservesOrder() {
    Map<String, String> fakeStore = new HashMap<>();
    useFakeRedis(fakeStore);

    List<ChatMessage> messages = List.of(
        UserMessage.from("What is my claim status?"),
        AiMessage.from("Let me check that for you."),
        ToolExecutionResultMessage.from("call-1", "getClaimStatusById", "PENDING")
    );

    store.updateMessages("user1", messages);
    List<ChatMessage> result = store.getMessages("user1");

    assertThat(result).hasSize(3);
    assertThat(result.get(0)).isInstanceOf(UserMessage.class);
    assertThat(result.get(1)).isInstanceOf(AiMessage.class);
    assertThat(result.get(2)).isInstanceOf(ToolExecutionResultMessage.class);
  }

  // --- catch: getMessages — IllegalArgumentException (unknown type in stored JSON) ---

  @Test
  void getMessages_returnsEmptyList_onUnknownMessageType() {
    when(valueOps.get("chat:memory:user1"))
        .thenReturn("[{\"type\":\"UNKNOWN\",\"text\":\"hello\"}]");

    assertThat(store.getMessages("user1")).isEmpty();
  }

  // --- catch: fromRecord AI case — corrupt toolRequests JSON ---

  @Test
  void getMessages_throwsRuntimeException_onCorruptToolRequestsJson() {
    when(valueOps.get("chat:memory:user1"))
        .thenReturn("[{\"type\":\"AI\",\"text\":\"\",\"toolRequests\":\"not valid json {{\"}]");

    assertThatThrownBy(() -> store.getMessages("user1"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to deserialize tool requests");
  }

  // --- catch: toRecord default — unsupported ChatMessage implementation ---

  @Test
  void updateMessages_throwsIllegalArgumentException_onUnsupportedMessageType() {
    ChatMessage unknownMessage = mock(ChatMessage.class);

    assertThatThrownBy(() -> store.updateMessages("user1", List.of(unknownMessage)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported message type");
  }

  // --- branch: AiMessage with both text and tool requests ---

  @Test
  void roundTrip_aiMessage_withTextAndToolRequests() {
    Map<String, String> fakeStore = new HashMap<>();
    useFakeRedis(fakeStore);

    ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
        .id("call-1")
        .name("getClaimStatusById")
        .arguments("{\"claimId\":\"CLM-001\"}")
        .build();
    AiMessage aiMessage = AiMessage.from("Let me check that.", List.of(toolRequest));

    store.updateMessages("user1", List.of(aiMessage));
    List<ChatMessage> result = store.getMessages("user1");

    assertThat(result).hasSize(1);
    AiMessage restored = (AiMessage) result.get(0);
    assertThat(restored.text()).isEqualTo("Let me check that.");
    assertThat(restored.hasToolExecutionRequests()).isTrue();
    assertThat(restored.toolExecutionRequests().get(0).name()).isEqualTo("getClaimStatusById");
  }

  // --- catch: updateMessages line 54-55 — ObjectMapper fails on full records list ---

  @Test
  void updateMessages_throwsRuntimeException_onSerializationFailure() throws JsonProcessingException {
    ObjectMapper failingMapper = mock(ObjectMapper.class);
    when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("simulated") {});
    RedisChatMemoryStore failingStore = new RedisChatMemoryStore(
        mock(StringRedisTemplate.class), failingMapper);

    assertThatThrownBy(() -> failingStore.updateMessages("user1", List.of(UserMessage.from("hello"))))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to serialize chat messages");
  }

  // --- catch: toRecord AI case line 77-78 — ObjectMapper fails on tool requests ---

  @Test
  void updateMessages_throwsRuntimeException_onToolRequestsSerializationFailure() throws JsonProcessingException {
    ObjectMapper failingMapper = mock(ObjectMapper.class);
    when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("simulated") {});
    RedisChatMemoryStore failingStore = new RedisChatMemoryStore(
        mock(StringRedisTemplate.class), failingMapper);

    ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
        .id("call-1").name("getClaimStatusById").arguments("{}")
        .build();

    assertThatThrownBy(() -> failingStore.updateMessages("user1", List.of(AiMessage.from(List.of(toolRequest)))))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to serialize tool requests");
  }

  // --- deleteMessages ---

  @Test
  void deleteMessages_deletesCorrectKey() {
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    RedisChatMemoryStore localStore = new RedisChatMemoryStore(redisTemplate);

    localStore.deleteMessages("user1");

    verify(redisTemplate).delete("chat:memory:user1");
  }
}
