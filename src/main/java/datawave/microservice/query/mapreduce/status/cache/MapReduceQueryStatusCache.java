package datawave.microservice.query.mapreduce.status.cache;

import datawave.microservice.authorization.user.ProxiedUserDetails;
import datawave.microservice.cached.LockableCacheInspector;
import datawave.microservice.query.mapreduce.status.MapReduceQueryStatus;
import datawave.webservice.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.util.MultiValueMap;

import static datawave.microservice.query.mapreduce.status.MapReduceQueryStatus.MapReduceQueryState.DEFINED;
import static datawave.microservice.query.mapreduce.status.cache.MapReduceQueryStatusCache.CACHE_NAME;

@CacheConfig(cacheNames = CACHE_NAME)
public class MapReduceQueryStatusCache extends LockableCache<MapReduceQueryStatus> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public static final String CACHE_NAME = "mapReduceQueryStatusCache";
    
    public MapReduceQueryStatusCache(LockableCacheInspector cacheInspector) {
        super(cacheInspector, CACHE_NAME);
    }
    
    @CachePut(key = "#id")
    public MapReduceQueryStatus create(String id, String jobName, MultiValueMap<String,String> parameters, Query query, ProxiedUserDetails currentUser) {
        MapReduceQueryStatus mapReduceQueryStatus = new MapReduceQueryStatus();
        mapReduceQueryStatus.setId(id);
        mapReduceQueryStatus.setState(DEFINED);
        mapReduceQueryStatus.setJobName(jobName);
        mapReduceQueryStatus.setParameters(parameters);
        mapReduceQueryStatus.setCurrentUser(currentUser);
        if (query != null) {
            mapReduceQueryStatus.setQuery(query);
        }
        mapReduceQueryStatus.setLastUpdatedMillis(System.currentTimeMillis());
        return mapReduceQueryStatus;
    }
    
    @Override
    public MapReduceQueryStatus get(String mrQueryId) {
        return cacheInspector.list(CACHE_NAME, MapReduceQueryStatus.class, mrQueryId);
    }
    
    @Override
    @CachePut(key = "#id")
    public MapReduceQueryStatus update(String id, MapReduceQueryStatus mapReduceQueryStatus) {
        mapReduceQueryStatus.setLastUpdatedMillis(System.currentTimeMillis());
        return mapReduceQueryStatus;
    }
    
    @CacheEvict(key = "#id")
    public void remove(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Evicting id {}", id);
        }
    }
}
