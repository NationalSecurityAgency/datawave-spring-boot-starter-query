package datawave.microservice.query.util;

import datawave.microservice.query.config.QueryProperties;
import datawave.microservice.query.storage.QueryQueueManager;
import datawave.microservice.query.storage.QueryStatus;
import datawave.microservice.query.storage.QueryStorageCache;
import datawave.microservice.query.storage.QueryStorageLock;
import datawave.webservice.query.exception.DatawaveErrorCode;
import datawave.webservice.query.exception.NotFoundQueryException;
import datawave.webservice.query.exception.QueryException;

import static datawave.microservice.query.storage.QueryStatus.QUERY_STATE.CREATED;

public class QueryStatusUpdateUtil {
    
    private final QueryProperties queryProperties;
    private final QueryStorageCache queryStorageCache;
    
    public QueryStatusUpdateUtil(QueryProperties queryProperties, QueryStorageCache queryStorageCache) {
        this.queryProperties = queryProperties;
        this.queryStorageCache = queryStorageCache;
    }
    
    public void claimNextCall(QueryStatus queryStatus) throws QueryException {
        // we can only call next on a created query
        if (queryStatus.getQueryState() == CREATED) {
            // increment the concurrent next count
            if (queryStatus.getActiveNextCalls() < queryProperties.getNextCall().getConcurrency()) {
                queryStatus.setActiveNextCalls(queryStatus.getActiveNextCalls() + 1);
                
                // update the last used datetime for the query
                queryStatus.setLastUsedMillis(System.currentTimeMillis());
            } else {
                throw new QueryException(DatawaveErrorCode.QUERY_LOCKED_ERROR,
                                "Concurrent next call limit reached: " + queryProperties.getNextCall().getConcurrency());
            }
        } else {
            throw new QueryException(DatawaveErrorCode.NO_QUERY_OBJECT_MATCH, "Unable to find query status in cache with CREATED state.");
        }
    }
    
    public void releaseNextCall(QueryStatus queryStatus, QueryQueueManager queryQueueManager) throws QueryException {
        // decrement the concurrent next count
        if (queryStatus.getActiveNextCalls() > 0) {
            queryStatus.setActiveNextCalls(queryStatus.getActiveNextCalls() - 1);
            
            // update the last used datetime for the query
            queryStatus.setLastUsedMillis(System.currentTimeMillis());
            
            // TODO: We should add a 'queueExists' call to determine whether this needs to be run
            // if by the end of the call the query is no longer running, delete the results queue
            if (!queryStatus.isRunning()) {
                queryQueueManager.deleteQueue(queryStatus.getQueryKey().getQueryId());
            }
        } else {
            throw new QueryException(DatawaveErrorCode.QUERY_LOCKED_ERROR, "Concurrent next count can't be decremented: " + queryStatus.getActiveNextCalls());
        }
    }
    
    public QueryStatus lockedUpdate(String queryUUID, QueryStatusUpdater updater) throws QueryException, InterruptedException {
        QueryStatus queryStatus = null;
        QueryStorageLock statusLock = queryStorageCache.getQueryStatusLock(queryUUID);
        if (statusLock.tryLock(queryProperties.getLockWaitTimeMillis(), queryProperties.getLockLeaseTimeMillis())) {
            try {
                queryStatus = queryStorageCache.getQueryStatus(queryUUID);
                if (queryStatus != null) {
                    updater.apply(queryStatus);
                    queryStorageCache.updateQueryStatus(queryStatus);
                } else {
                    throw new NotFoundQueryException(DatawaveErrorCode.NO_QUERY_OBJECT_MATCH, "Unable to find query status in cache.");
                }
            } finally {
                statusLock.unlock();
            }
        } else {
            updater.onLockFailed();
        }
        return queryStatus;
    }
}
