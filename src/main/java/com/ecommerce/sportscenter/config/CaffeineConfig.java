package com.ecommerce.sportscenter.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * CaffeineConfig - Configuration du cache local (L1)
 *
 * 🎯 CONCEPT: Multi-Level Caching
 *
 * L1 (Caffeine) → L2 (Redis) → Database
 *    1-5ms         10-20ms       300-500ms
 *
 * POURQUOI CAFFEINE?
 * 1. Ultra-rapide (in-memory, même JVM)
 * 2. Réduit les appels réseau à Redis
 * 3. Haute concurrence (lock-free)
 *
 * QUAND L'UTILISER?
 * - Données lues TRÈS fréquemment (hot data)
 * - Acceptable si légèrement obsolète (eventual consistency)
 * - Ex: Top 100 produits, categories, config
 *
 * LIMITES:
 * - Local à 1 serveur (pas partagé)
 * - Taille limitée (RAM)
 * - Invalidation complexe en multi-serveurs
 */
@Configuration
@EnableCaching
public class CaffeineConfig {

    /**
     * CacheManager pour Caffeine (L1 cache)
     *
     * PARAMÈTRES EXPLIQUÉS:
     *
     * maximumSize(10_000):
     * - Limite à 10K entrées
     * - Protection contre OutOfMemoryError
     * - Éviction LRU (Least Recently Used)
     *
     * expireAfterWrite(5 minutes):
     * - TTL court pour fraîcheur
     * - Plus court que Redis (stratégie conservative)
     *
     * expireAfterAccess(3 minutes):
     * - Reset TTL si utilisé
     * - Garde le hot data plus longtemps
     *
     * recordStats():
     * - Track hit/miss ratio
     * - Essentiel pour monitoring
     */
    @Bean(name = "caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "productById",      // Cache des produits individuels
                "categories",       // Cache des catégories (très stable)
                "topProducts"       // Top produits (hot data)
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                // Taille max (protection mémoire)
                .maximumSize(10_000)

                // TTL absolu
                .expireAfterWrite(5, TimeUnit.MINUTES)

                // TTL relatif (reset si accès)
                .expireAfterAccess(3, TimeUnit.MINUTES)

                // Métriques (crucial pour monitoring)
                .recordStats()

                // Soft values = éviction si GC pressure
                .softValues()
        );

        return cacheManager;
    }

    /**
     * Configuration pour données ultra-stables
     * Ex: Configuration système, categories racines
     */
    @Bean(name = "longTermCaffeineCache")
    public CacheManager longTermCaffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("longTermCache");

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(1, TimeUnit.HOURS) // TTL long
                .recordStats()
        );

        return cacheManager;
    }

    /**
     * Configuration pour données volatiles
     * Ex: Résultats de recherche, suggestions
     */
    @Bean(name = "shortTermCaffeineCache")
    public CacheManager shortTermCaffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("searchCache");

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(1, TimeUnit.MINUTES) // TTL très court
                .recordStats()
        );

        return cacheManager;
    }
}