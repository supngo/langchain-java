package com.naturecode.langchain.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer()));

    return RedisCacheManager.builder(factory)
        .cacheDefaults(base.entryTtl(Duration.ofMinutes(10)))
        .withInitialCacheConfigurations(Map.of(
            "rag", base.entryTtl(Duration.ofHours(1))
        ))
        .build();
  }

  private static RedisSerializer<Object> jsonSerializer() {
    ObjectMapper mapper = new ObjectMapper();

    return new RedisSerializer<>() {
      @Override
      public byte[] serialize(Object value) throws SerializationException {
        if (value == null) return new byte[0];
        try {
          return mapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
          throw new SerializationException("JSON serialization failed", e);
        }
      }

      @Override
      public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) return null;
        try {
          return mapper.readValue(bytes, Object.class);
        } catch (IOException e) {
          throw new SerializationException("JSON deserialization failed", e);
        }
      }
    };
  }
}
