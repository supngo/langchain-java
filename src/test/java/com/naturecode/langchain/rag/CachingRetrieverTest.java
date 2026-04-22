package com.naturecode.langchain.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;

class CachingRetrieverTest {

  private EmbeddingStoreContentRetriever delegate;
  private Cache cache;
  private CachingRetriever cachingRetriever;

  @BeforeEach
  public void setUp() {
    delegate = mock(EmbeddingStoreContentRetriever.class);
    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache("rag")).thenReturn(cache);
    cachingRetriever = new CachingRetriever(delegate, cacheManager);
  }

  // --- cache miss ---

  @Test
  void retrieve_cacheMiss_delegatesToRetriever() {
    Query query = Query.from("What is the claim deadline?");
    List<Content> delegateResult = List.of(
        Content.from(TextSegment.from("Claims must be submitted within 30 days.")));

    when(cache.get("What is the claim deadline?")).thenReturn(null);
    when(delegate.retrieve(query)).thenReturn(delegateResult);

    List<Content> result = cachingRetriever.retrieve(query);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).textSegment().text())
        .isEqualTo("Claims must be submitted within 30 days.");
    verify(delegate).retrieve(query);
  }

  @Test
  void retrieve_cacheMiss_storesOnlyTextsInCache() {
    Query query = Query.from("fraud policy");
    when(cache.get("fraud policy")).thenReturn(null);
    when(delegate.retrieve(query)).thenReturn(List.of(
        Content.from(TextSegment.from("Claims are denied if fraud is detected."))));

    cachingRetriever.retrieve(query);

    verify(cache).put(eq("fraud policy"), eq(List.of("Claims are denied if fraud is detected.")));
  }

  @Test
  void retrieve_cacheMiss_storesMultipleTexts() {
    Query query = Query.from("policy");
    when(cache.get("policy")).thenReturn(null);
    when(delegate.retrieve(query)).thenReturn(List.of(
        Content.from(TextSegment.from("Policy A")),
        Content.from(TextSegment.from("Policy B"))));

    cachingRetriever.retrieve(query);

    verify(cache).put(eq("policy"), eq(List.of("Policy A", "Policy B")));
  }

  // --- cache hit ---

  @Test
  void retrieve_cacheHit_doesNotCallDelegate() {
    Query query = Query.from("What is the claim deadline?");
    Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
    when(wrapper.get()).thenReturn(List.of("Claims must be submitted within 30 days."));
    when(cache.get("What is the claim deadline?")).thenReturn(wrapper);

    cachingRetriever.retrieve(query);

    verify(delegate, never()).retrieve(any());
  }

  @Test
  void retrieve_cacheHit_reconstructsContentFromTexts() {
    Query query = Query.from("What is the claim deadline?");
    Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
    when(wrapper.get()).thenReturn(List.of("Claims must be submitted within 30 days."));
    when(cache.get("What is the claim deadline?")).thenReturn(wrapper);

    List<Content> result = cachingRetriever.retrieve(query);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).textSegment().text())
        .isEqualTo("Claims must be submitted within 30 days.");
  }

  @Test
  void retrieve_cacheHit_reconstructsMultipleSegments() {
    Query query = Query.from("policy");
    Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
    when(wrapper.get()).thenReturn(List.of("Policy A", "Policy B", "Policy C"));
    when(cache.get("policy")).thenReturn(wrapper);

    List<Content> result = cachingRetriever.retrieve(query);

    assertThat(result).hasSize(3);
    assertThat(result.get(0).textSegment().text()).isEqualTo("Policy A");
    assertThat(result.get(1).textSegment().text()).isEqualTo("Policy B");
    assertThat(result.get(2).textSegment().text()).isEqualTo("Policy C");
  }
}
