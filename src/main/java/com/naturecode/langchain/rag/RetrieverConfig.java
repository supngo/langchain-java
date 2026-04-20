package com.naturecode.langchain.rag;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

@Configuration
public class RetrieverConfig {

  @Bean
  public EmbeddingStoreContentRetriever embeddingStoreContentRetriever(KnowledgeBase kb, EmbeddingModel embeddingModel) {
    return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(kb.getStore())
            .embeddingModel(embeddingModel)
            .maxResults(3)
            .build();
  }
}