# Technical Guide

Detailed documentation for the E-Commerce Distributed Caching Solution.

---

## Key Concepts

### 1. Multi-Level Caching

#### Why 2 Levels?

**L1 - Caffeine (Local):**
- Ultra-fast (1-5ms)
- No network latency
- Limited to 1 server
- Limited memory size

**L2 - Redis (Distributed):**
- Shared across all servers
- Optional persistence
- Horizontally scalable
- Network latency (10-20ms)

**Request Flow:**
```
1. Check Caffeine → Hit? Return (1-5ms)
2. Check Redis → Hit? Return + Cache L1 (10-20ms)
3. Query DB → Cache L2 + L1
```

### 2. Cache-Aside Pattern

```java
@Cacheable(value = "products", key = "#id")
public ProductDTO getProductById(Long id) {
    // On cache miss → query DB
    // Spring auto-caches the result
    return repository.findById(id);
}
```

### 3. Distributed Locks (Redisson)

**Problem - Cache Stampede:**
```
Cache expires → 1000 simultaneous requests → 1000 DB queries
```

**Solution:**
```java
RLock lock = redissonClient.getLock("lock:product:" + id);
if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
    try {
        // Only 1 thread queries the DB
        // 999 others wait for the result
    } finally {
        lock.unlock();
    }
}
```

### 4. Cache Warming

```java
@EventListener(ApplicationReadyEvent.class)
public void warmCacheOnStartup() {
    // Pre-load top 100 products
    // Pre-load categories
    // Pre-load new arrivals
}
```

---

## manage.sh Commands

| Command | Description |
|---------|-------------|
| `./manage.sh start` | Start all services (Docker + Spring Boot) |
| `./manage.sh stop` | Stop all services |
| `./manage.sh restart` | Restart all services |
| `./manage.sh build` | Build the application |
| `./manage.sh test` | Run load tests |
| `./manage.sh demo` | Full demo (build + start + test) |
| `./manage.sh status` | Check service status |
| `./manage.sh metrics` | Show current metrics |
| `./manage.sh logs` | Show application logs |
| `./manage.sh clean` | Clean artifacts and Docker volumes |

---

## Load Testing

```bash
chmod +x load-test.sh
./load-test.sh
```

### Expected Results

**Baseline (No Cache):**
```
Requests per second:    ~1000 req/s
Time per request:       23ms (mean)
```

**Warm Cache (Multi-Level):**
```
Requests per second:    11,695 req/s    ← 11x improvement
Time per request:       4ms (mean)      ← 83% faster
```

**High Load:**
```
Requests per second:    18,173 req/s   ← Peak throughput
Concurrency:           100
```

### Manual Tests

```bash
# Get product
curl http://localhost:8080/api/products/1

# Get by category
curl http://localhost:8080/api/products/category/Electronics

# Search
curl "http://localhost:8080/api/products/search?keyword=phone"

# With distributed lock
curl http://localhost:8080/api/products/1/with-lock
```

---

## API Endpoints

### Products

| Method | Endpoint | Cache | Description |
|--------|----------|-------|-------------|
| GET | `/api/products` | Redis 10min | All products |
| GET | `/api/products/{id}` | L1+L2 15min | Product by ID |
| GET | `/api/products/{id}/with-lock` | L2 + Lock | With distributed lock |
| GET | `/api/products/category/{cat}` | Redis 10min | By category |
| GET | `/api/products/search?keyword=` | Redis 5min | Search |
| GET | `/api/products/price-range?min=&max=` | Redis 3min | Price range |
| GET | `/api/products/categories` | Redis 1h | All categories |
| POST | `/api/products` | Invalidates | Create product |
| PUT | `/api/products/{id}` | Updates cache | Update product |
| DELETE | `/api/products/{id}` | Clears cache | Delete product |

### Monitoring

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health check |
| `/actuator/prometheus` | Prometheus metrics |
| `/actuator/metrics` | All metrics |
| `/actuator/caches` | Cache statistics |

---

## Monitoring & Metrics

### Grafana

1. Open: http://localhost:3000
2. Login: `admin` / `admin`
3. Dashboard: "E-Commerce Cache Performance"

### Key Metrics

| Metric | Description | Target |
|--------|-------------|--------|
| `cache_hit_total` | Cache hits | > 95% |
| `cache_miss_total` | Cache misses | < 5% |
| `database_query_total` | DB queries | Decreasing |
| `http_server_requests` | Response time | < 10ms P95 |

### Prometheus Queries

```promql
# Cache hit ratio
rate(cache_hit_total[5m]) / (rate(cache_hit_total[5m]) + rate(cache_miss_total[5m])) * 100

# Average response time
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# Requests per second
rate(http_server_requests_seconds_count[5m])
```

---

## Best Practices

### Cache TTL Strategy

```java
// Stable data (categories) → Long TTL
cacheConfigurations.put("categories",
    defaultConfig.entryTtl(Duration.ofHours(1)));

// Frequent data (products) → Medium TTL
cacheConfigurations.put("products",
    defaultConfig.entryTtl(Duration.ofMinutes(10)));

// Volatile data (search) → Short TTL
cacheConfigurations.put("searchResults",
    defaultConfig.entryTtl(Duration.ofMinutes(5)));
```

### Cache Invalidation

```java
@Caching(evict = {
    @CacheEvict(value = "productById", key = "#id"),
    @CacheEvict(value = "products", allEntries = true)
})
public void updateProduct(Long id, ProductDTO dto) {
    // Update logic
}
```

### Cache Keys Design

```
Good:  "product:123"
       "category:Electronics"
       "search:iphone"

Bad:   "getAllProducts"  ← Non-unique
       "data"            ← Too vague
```

---

## Troubleshooting

### Redis Connection Failed

```bash
docker-compose ps redis
docker-compose logs redis
docker-compose restart redis
```

### Cache Not Working

```bash
curl http://localhost:8080/actuator/caches
curl -X DELETE http://localhost:8080/actuator/caches
```

### High Memory Usage

```bash
curl http://localhost:8080/actuator/metrics/cache.size
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

---

## Resources

- [Spring Cache Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Redis Best Practices](https://redis.io/docs/manual/patterns/)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [Redisson Documentation](https://redisson.org/)
- [Micrometer Metrics](https://micrometer.io/)
