package com.ecommerce.sportscenter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * RedisConfig - Configuration Redis & Redisson
 *
 * CONCEPTS CLÉS:
 * 1. Redis = Cache distribué (partagé entre tous les serveurs)
 * 2. Redisson = Client Redis avancé avec distributed locks
 * 3. TTL = Time To Live (durée de vie du cache)
 *
 * POURQUOI DES TTL DIFFÉRENTS?
 * - Products: 10 min (changent rarement)
 * - Categories: 1 heure (très stables)
 * - Search: 5 min (résultats peuvent changer)
 */
@Configuration
public class RedisConfig {

    /**
     * RedisConnectionFactory - Connexion à Redis
     * Utilise Lettuce (client Redis performant et thread-safe)
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6379);
        // config.setPassword("your-redis-password"); // En production

        return new LettuceConnectionFactory(config);
    }

    /**
     * ObjectMapper personnalisé pour Redis
     * Support des dates Java 8+ (LocalDateTime, etc.)
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * RedisTemplate - Pour manipuler Redis directement
     * Utilisé pour des opérations custom (distributed locks, counters, etc.)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Sérialisation
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedissonClient - Pour distributed locks
     *
     * POURQUOI? Éviter le "Cache Stampede"
     * Scénario: 1000 requêtes simultanées pour un produit non-caché
     * Sans lock: 1000 queries DB
     * Avec lock: 1 query DB, 999 attendent le résultat
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setConnectionPoolSize(50)
                .setConnectionMinimumIdleSize(10)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        return Redisson.create(config);
    }

    /**
     * RedisCacheManager - Gère les caches Redis avec TTL
     *
     * STRATÉGIE DE TTL:
     * - Données stables (categories) = TTL long
     * - Données fréquentes (products) = TTL moyen
     * - Données volatiles (search) = TTL court
     */
    @Bean
    @Primary
    public CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper) {

        // Configuration par défaut
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper)))
                .disableCachingNullValues();

        // Configurations spécifiques par cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Products: 10 minutes (lu souvent, change rarement)
        cacheConfigurations.put("products",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Product by ID: 15 minutes (très stable)
        cacheConfigurations.put("productById",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Categories: 1 heure (quasi-statique)
        cacheConfigurations.put("categories",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // Search results: 5 minutes (résultats peuvent changer)
        cacheConfigurations.put("searchResults",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Price range: 3 minutes (prix fluctuent)
        cacheConfigurations.put("priceRange",
                defaultConfig.entryTtl(Duration.ofMinutes(3)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * RedissonSpringCacheManager - Alternative avec Redisson
     * Utilisé pour des cas avancés (distributed locks intégrés)
     */
    @Bean(name = "redissonCacheManager")
    public CacheManager redissonCacheManager(RedissonClient redissonClient) {
        Map<String, org.redisson.spring.cache.CacheConfig> config = new HashMap<>();

        // Configuration avec Redisson
        config.put("distributedCache",
                new org.redisson.spring.cache.CacheConfig(
                        Duration.ofMinutes(10).toMillis(),
                        Duration.ofMinutes(5).toMillis()
                ));

        return new RedissonSpringCacheManager(redissonClient, config);
    }
}