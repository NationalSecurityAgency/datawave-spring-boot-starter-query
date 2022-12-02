package datawave.microservice.query.mapreduce.status.cache;

import datawave.microservice.cached.LockableCacheInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;

import java.util.Set;

import static datawave.microservice.query.mapreduce.status.cache.MapReduceQueryIdByUsernameCache.CACHE_NAME;

@CacheConfig(cacheNames = CACHE_NAME)
public class MapReduceQueryIdByUsernameCache extends LockableCache<Set<String>> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public static final String CACHE_NAME = "mapReduceQueryIdByUsernameCache";
    
    public MapReduceQueryIdByUsernameCache(LockableCacheInspector cacheInspector) {
        super(cacheInspector, CACHE_NAME);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Set<String> get(String username) {
        return (Set<String>) cacheInspector.list(CACHE_NAME, Set.class, username);
    }
    
    @Override
    @CachePut(key = "#username")
    public Set<String> update(String username, Set<String> ids) {
        if (log.isDebugEnabled()) {
            log.debug("Updating mrQueryIds for username {}", username);
        }
        return ids;
    }
    
    @CacheEvict(key = "#username")
    public void removeAll(String username) {
        if (log.isDebugEnabled()) {
            log.debug("Evicting username {}", username);
        }
    }
}
