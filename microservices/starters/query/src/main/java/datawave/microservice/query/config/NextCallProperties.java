package datawave.microservice.query.config;

import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.concurrent.TimeUnit;

@Validated
public class NextCallProperties {
    @PositiveOrZero
    private long resultPollInterval = TimeUnit.SECONDS.toMillis(6);
    @NotNull
    private TimeUnit resultPollTimeUnit = TimeUnit.MILLISECONDS;
    @Positive
    private int concurrency = 1;
    @PositiveOrZero
    private long statusUpdateInterval = TimeUnit.SECONDS.toMillis(6);
    @NotNull
    private TimeUnit statusUpdateTimeUnit = TimeUnit.MILLISECONDS;
    @PositiveOrZero
    private long maxResultsTimeout = 5l;
    @NotNull
    private TimeUnit maxResultsTimeUnit = TimeUnit.SECONDS;
    
    private ThreadPoolTaskExecutorProperties executor = new ThreadPoolTaskExecutorProperties(10, 100, 100, "nextCall-");
    
    public long getResultPollInterval() {
        return resultPollInterval;
    }
    
    public long getResultPollIntervalMillis() {
        return resultPollTimeUnit.toMillis(resultPollInterval);
    }
    
    public void setResultPollInterval(long resultPollInterval) {
        this.resultPollInterval = resultPollInterval;
    }
    
    public TimeUnit getResultPollTimeUnit() {
        return resultPollTimeUnit;
    }
    
    public void setResultPollTimeUnit(TimeUnit resultPollTimeUnit) {
        this.resultPollTimeUnit = resultPollTimeUnit;
    }
    
    public int getConcurrency() {
        return concurrency;
    }
    
    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }
    
    public long getStatusUpdateInterval() {
        return statusUpdateInterval;
    }
    
    public long getStatusUpdateIntervalMillis() {
        return statusUpdateTimeUnit.toMillis(statusUpdateInterval);
    }
    
    public void setStatusUpdateInterval(long statusUpdateInterval) {
        this.statusUpdateInterval = statusUpdateInterval;
    }
    
    public TimeUnit getStatusUpdateTimeUnit() {
        return statusUpdateTimeUnit;
    }
    
    public void setStatusUpdateTimeUnit(TimeUnit statusUpdateTimeUnit) {
        this.statusUpdateTimeUnit = statusUpdateTimeUnit;
    }
    
    public long getMaxResultsTimeout() {
        return maxResultsTimeout;
    }
    
    public long getMaxResultsTimeoutMillis() {
        return maxResultsTimeUnit.toMillis(maxResultsTimeout);
    }
    
    public void setMaxResultsTimeout(long maxResultsTimeout) {
        this.maxResultsTimeout = maxResultsTimeout;
    }
    
    public TimeUnit getMaxResultsTimeUnit() {
        return maxResultsTimeUnit;
    }
    
    public void setMaxResultsTimeUnit(TimeUnit maxResultsTimeUnit) {
        this.maxResultsTimeUnit = maxResultsTimeUnit;
    }
    
    public ThreadPoolTaskExecutorProperties getExecutor() {
        return executor;
    }
    
    public void setExecutor(ThreadPoolTaskExecutorProperties executor) {
        this.executor = executor;
    }
}
