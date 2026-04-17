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

Requires `OPENAI_API_KEY` environment variable to be set.

## Architecture

Spring Boot 4.0 + LangChain4j 1.13 REST API for AI-powered RAG (Retrieval-Augmented Generation).

**Request flow:** `AIController` → `AIAssistant` (service) → `Assistant` (LangChain4j AiServices interface) → tools/RAG/LLM

**Key packages under `com.naturecode.langchain`:**

- `controller/` — `POST /ai/ask` accepts `{"userId": "...", "question": "..."}`, returns string response
- `service/AIAssistant.java` — builds an `AiServices` instance per call with per-user memory, retriever, and tools; `ask(userId, question)` is the entry point
- `agent/Assistant.java` — LangChain4j `@AiService` interface; defines the `chat(String)` method
- `config/LLMConfig.java` — `ChatModel` (gpt-4o-mini) and `EmbeddingModel` (text-embedding-3-small) beans, both via `OPENAI_API_KEY`
- `memory/MemoryStore.java` — `ConcurrentHashMap`-backed store that lazily creates a `MessageWindowChatMemory` (20 messages) per `userId`; replaces the old single shared memory
- `rag/` — `KnowledgeBase` loads sample insurance policy docs into an in-memory embedding store at startup; `RetrieverConfig` sets up `EmbeddingStoreContentRetriever` (max 3 results)
- `tool/ClaimService.java` — two `@Tool` methods: `getClaimStatusById` (`/claims/{claimId}`) and `getClaimStatusByCustomer` (`/claims/?customerId=`), both hitting `http://localhost:8081`

Chat memory is now per-user: each unique `userId` gets its own isolated conversation history.
