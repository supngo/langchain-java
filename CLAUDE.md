# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build          # Assemble and test
./gradlew bootRun        # Run locally
./gradlew test           # Run all tests
./gradlew test --tests com.naturecode.langchain.SomeTest.methodName  # Run single test
./gradlew clean          # Clean build directory
```

Requires `OPENAI_API_KEY` environment variable and a running Redis instance (default: `localhost:6379`).

## Architecture

Spring Boot 4.0 + LangChain4j 1.0.0 REST API for AI-powered RAG (Retrieval-Augmented Generation), with reactive (WebFlux) I/O and Redis-backed persistence.

**Request flow:**
```
POST /ai/ask
  → AIController (returns Mono<String>)
  → AIAssistant.ask() — wraps blocking LangChain4j call in Mono.fromCallable() on boundedElastic scheduler
    → builds Assistant via AiServices per request
      → ChatModel (gpt-4o-mini) + CachingRetriever (RAG) + ClaimService (tools) + per-user ChatMemory
```

**Key packages under `com.naturecode.langchain`:**

- `controller/` — `POST /ai/ask` accepts `{"userId": "...", "question": "..."}`, returns string response
- `service/AIAssistant.java` — assembles a fresh `AiServices` instance per call; `ask(userId, question)` is the entry point; returns `Mono<String>`
- `agent/Assistant.java` — LangChain4j `@AiService` marker interface; implementation is created at runtime by `AiServices.builder()`
- `config/LLMConfig.java` — `ChatModel` (gpt-4o-mini, temp 0.2) and `EmbeddingModel` (text-embedding-3-small) beans
- `config/CacheConfig.java` — `@EnableCaching`; `RedisCacheManager` with JSON serializer; default TTL 10 min, RAG cache TTL 1 hour
- `memory/MemoryStore.java` — lazily creates a `MessageWindowChatMemory` (20 messages) per `userId`, backed by `RedisChatMemoryStore`
- `memory/RedisChatMemoryStore.java` — `ChatMemoryStore` implementation using `StringRedisTemplate`; keys are `chat:memory:{userId}`; serializes messages to JSON
- `rag/KnowledgeBase.java` — loads 2 hardcoded insurance policy documents into an `InMemoryEmbeddingStore` at startup
- `rag/RetrieverConfig.java` — `EmbeddingStoreContentRetriever` bean (max 3 results)
- `rag/CachingRetriever.java` — decorator over `RetrieverConfig`; caches query→content results in Redis (1-hour TTL) to avoid re-embedding identical queries
- `tool/ClaimService.java` — two `@Tool` methods (`getClaimStatusById`, `getClaimStatusByCustomer`) calling `http://localhost:8081`; uses WebClient with 2s timeout, 1 retry, and `onErrorResume` fallback

## Caching

Two layers of Redis caching:
1. **RAG cache** (`CachingRetriever`): query text → retrieved documents, 1-hour TTL, cache name `"rag"`
2. **Chat memory** (`RedisChatMemoryStore`): per-userId conversation history, no TTL

`CacheConfig` provides a custom `ObjectMapper`-based JSON serializer so cached values are human-readable in Redis.

## Testing

Tests live in `src/test/java/com/naturecode/langchain/`. JaCoCo coverage is configured with exclusions for `config/`, `dto/`, `agent/`, `rag/`, `memory/MemoryStore`, and the application class.

- **AIControllerTest** — Mockito unit tests; uses `StepVerifier` for reactive assertions
- **RedisChatMemoryStoreTest** — round-trip tests for all message types using a fake in-memory map; covers corrupt JSON and unsupported type errors
- **CachingRetrieverTest** — cache hit/miss scenarios with Mockito
- **ClaimServiceTest** — `MockWebServer` (okhttp3) for HTTP stub testing; validates retry and fallback behavior
- **LangchainApplicationTests** — context load test; disabled unless live OpenAI key and Redis are available

## Runtime Dependencies

- **Redis** at `localhost:6379` (configurable via `spring.data.redis.*` in `application.properties`)
- **Claims API** at `http://localhost:8081` (for ClaimService tool calls)
- **OpenAI API key** (`OPENAI_API_KEY` env var)
