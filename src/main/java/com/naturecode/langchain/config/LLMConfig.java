package com.naturecode.langchain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

@Configuration
public class LLMConfig {

  @Bean
  public ChatModel chatModel() {
    return OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o-mini")
            .temperature(0.2)
            .build();
  }

  @Bean
  public EmbeddingModel embeddingModel() {
    return OpenAiEmbeddingModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("text-embedding-3-small")
            .build();
  }
}
