package com.bank.loanengine.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;

/**
 * Redis configuration enabling Spring Cache backed by Redis.
 *
 * <h3>Cache names and TTLs</h3>
 * <pre>
 * loans         → caches full loan + schedule responses           (TTL: 10 min)
 * loan-schedules → caches schedule-only queries                   (TTL: 10 min)
 * </pre>
 *
 * Both caches are evicted when a prepayment or mark-paid-up-to operation mutates a loan,
 * keeping Redis consistent with MySQL. Values are stored as JSON (not Java serialization) so
 * cache entries survive application restarts and are human-readable via redis-cli.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    public static final String CACHE_LOANS          = "loans";
    public static final String CACHE_LOAN_SCHEDULES = "loan-schedules";

    @Value("${app.cache.loans.ttl-minutes:10}")
    private long loansTtlMinutes;

    // ── ObjectMapper for Redis ────────────────────────────────────────────────────────────────

    @Bean
    public ObjectMapper redisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Embed type information so polymorphic types (including final records)
                // survive round-trips when stored as JSON in Redis.
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.EVERYTHING,
                        JsonTypeInfo.As.PROPERTY);
    }

    // ── RedisTemplate (for manual ops if needed) ─────────────────────────────────────────────

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                        ObjectMapper redisObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
        template.afterPropertiesSet();
        return template;
    }

        // Clear any existing cache keys which may have been stored with different typing
        // metadata on previous runs. This prevents LinkedHashMap -> DTO cast failures.
        @Bean
        public ApplicationRunner clearStaleCachesOnStartup(RedisTemplate<String, Object> template) {
                return args -> {
                        Logger log = LoggerFactory.getLogger(RedisConfig.class);
                        try {
                                Set<String> loanKeys = template.keys("*" + CACHE_LOANS + "*");
                                if (loanKeys != null && !loanKeys.isEmpty()) {
                                        template.delete(loanKeys);
                                        log.info("Cleared {} stale loan cache keys", loanKeys.size());
                                }

                                Set<String> scheduleKeys = template.keys("*" + CACHE_LOAN_SCHEDULES + "*");
                                if (scheduleKeys != null && !scheduleKeys.isEmpty()) {
                                        template.delete(scheduleKeys);
                                        log.info("Cleared {} stale loan-schedule cache keys", scheduleKeys.size());
                                }
                        } catch (Exception ex) {
                                log.warn("Failed to clear stale Redis cache keys on startup: {}", ex.getMessage());
                        }
                };
        }

    // ── CacheManager ─────────────────────────────────────────────────────────────────────────

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     ObjectMapper redisObjectMapper) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .entryTtl(Duration.ofMinutes(loansTtlMinutes))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_LOANS,          defaults);
        cacheConfigs.put(CACHE_LOAN_SCHEDULES, defaults);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            private final Logger log = LoggerFactory.getLogger(RedisConfig.class);

            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache GET error on cache '{}' key '{}': {}", cache.getName(), key, exception.getMessage());
                try {
                    if (key != null) {
                        cache.evict(key);
                        log.info("Evicted corrupted cache key '{}' from cache '{}'.", key, cache.getName());
                    }
                } catch (Exception ex) {
                    log.warn("Failed to evict corrupted cache key '{}': {}", key, ex.getMessage());
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis cache PUT error on cache '{}' key '{}': {}", cache.getName(), key, exception.getMessage());
                try {
                    if (key != null) {
                        cache.evict(key);
                        log.info("Evicted cache key '{}' after PUT failure from cache '{}'.", key, cache.getName());
                    }
                } catch (Exception ex) {
                    log.warn("Failed to evict cache key '{}' after PUT failure: {}", key, ex.getMessage());
                }
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache EVICT error on cache '{}' key '{}': {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis cache CLEAR error on cache '{}': {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
