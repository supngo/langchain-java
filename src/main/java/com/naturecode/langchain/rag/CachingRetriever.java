package com.naturecode.langchain.rag;

import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Primary
public class CachingRetriever implements ContentRetriever {

  private final EmbeddingStoreContentRetriever delegate;
  private final Cache cache;

  public CachingRetriever(EmbeddingStoreContentRetriever delegate, CacheManager cacheManager) {
    this.delegate = delegate;
    this.cache = cacheManager.getCache("rag");
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Content> retrieve(Query query) {
    String key = query.text();

    Cache.ValueWrapper wrapper = cache.get(key);
    if (wrapper != null) {
      log.debug("RAG cache hit query=\"{}\"", key);
      List<String> texts = (List<String>) wrapper.get();
      return texts.stream().map(t -> Content.from(TextSegment.from(t))).toList();
    }

    List<Content> results = delegate.retrieve(query);
    log.debug("RAG cache miss query=\"{}\" results={}", key, results.size());
    cache.put(key, results.stream().map(c -> c.textSegment().text()).toList());
    return results;
  }
}
