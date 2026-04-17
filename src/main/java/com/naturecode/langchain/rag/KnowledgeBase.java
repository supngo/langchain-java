package com.naturecode.langchain.rag;

import org.springframework.stereotype.Service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

@Service
public class KnowledgeBase {
  private final InMemoryEmbeddingStore<TextSegment> store;

  public KnowledgeBase(EmbeddingModel embeddingModel) {
    store = new InMemoryEmbeddingStore<>();

    TextSegment segment1 = TextSegment.from("Policy: Claims are denied if fraud is detected.");
    TextSegment segment2 = TextSegment.from("Policy: Claims must be submitted within 30 days.");

    store.add(embeddingModel.embed(segment1).content(), segment1);
    store.add(embeddingModel.embed(segment2).content(), segment2);
  }

  public EmbeddingStore<TextSegment> getStore() {
    return store;
  }
  
}
