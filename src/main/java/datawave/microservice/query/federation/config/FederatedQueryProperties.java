package datawave.microservice.query.federation.config;

import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

public class FederatedQueryProperties {
    private String queryServiceUri = "https://query:8443/query/v1/query";
    
    private String queryMetricServiceUri = "https://querymetric:8443/querymetric/v1/id";
    
    // max bytes to buffer for each rest call (-1 is unlimited)
    private int maxBytesToBuffer = -1;
    
    @PositiveOrZero
    private long createTimeout = TimeUnit.SECONDS.toMillis(30);
    
    @NotNull
    private TimeUnit createTimeUnit = TimeUnit.MILLISECONDS;
    
    @PositiveOrZero
    private long nextTimeout = TimeUnit.HOURS.toMillis(1);
    
    @NotNull
    private TimeUnit nextTimeUnit = TimeUnit.MILLISECONDS;
    
    @PositiveOrZero
    private long closeTimeout = TimeUnit.HOURS.toMillis(1);
    
    @NotNull
    private TimeUnit closeTimeUnit = TimeUnit.MILLISECONDS;
    
    @PositiveOrZero
    private long planTimeout = TimeUnit.MINUTES.toMillis(20);
    
    @NotNull
    private TimeUnit planTimeUnit = TimeUnit.MILLISECONDS;
    
    public String getQueryServiceUri() {
        return queryServiceUri;
    }
    
    public void setQueryServiceUri(String queryServiceUri) {
        this.queryServiceUri = queryServiceUri;
    }
    
    public String getQueryMetricServiceUri() {
        return queryMetricServiceUri;
    }
    
    public void setQueryMetricServiceUri(String queryMetricServiceUri) {
        this.queryMetricServiceUri = queryMetricServiceUri;
    }
    
    public int getMaxBytesToBuffer() {
        return maxBytesToBuffer;
    }
    
    public void setMaxBytesToBuffer(int maxBytesToBuffer) {
        this.maxBytesToBuffer = maxBytesToBuffer;
    }
    
    public long getCreateTimeout() {
        return createTimeout;
    }
    
    public long getCreateTimeoutMillis() {
        return createTimeUnit.toMillis(createTimeout);
    }
    
    public void setCreateTimeout(long createTimeout) {
        this.createTimeout = createTimeout;
    }
    
    public TimeUnit getCreateTimeUnit() {
        return createTimeUnit;
    }
    
    public void setCreateTimeUnit(TimeUnit createTimeUnit) {
        this.createTimeUnit = createTimeUnit;
    }
    
    public long getNextTimeout() {
        return nextTimeout;
    }
    
    public long getNextTimeoutMillis() {
        return nextTimeUnit.toMillis(nextTimeout);
    }
    
    public void setNextTimeout(long nextTimeout) {
        this.nextTimeout = nextTimeout;
    }
    
    public TimeUnit getNextTimeUnit() {
        return nextTimeUnit;
    }
    
    public void setNextTimeUnit(TimeUnit nextTimeUnit) {
        this.nextTimeUnit = nextTimeUnit;
    }
    
    public long getCloseTimeout() {
        return closeTimeout;
    }
    
    public long getCloseTimeoutMillis() {
        return closeTimeUnit.toMillis(closeTimeout);
    }
    
    public void setCloseTimeout(long closeTimeout) {
        this.closeTimeout = closeTimeout;
    }
    
    public TimeUnit getCloseTimeUnit() {
        return closeTimeUnit;
    }
    
    public void setCloseTimeUnit(TimeUnit closeTimeUnit) {
        this.closeTimeUnit = closeTimeUnit;
    }
    
    public long getPlanTimeout() {
        return planTimeout;
    }
    
    public long getPlanTimeoutMillis() {
        return planTimeUnit.toMillis(planTimeout);
    }
    
    public void setPlanTimeout(long planTimeout) {
        this.planTimeout = planTimeout;
    }
    
    public TimeUnit getPlanTimeUnit() {
        return planTimeUnit;
    }
    
    public void setPlanTimeUnit(TimeUnit planTimeUnit) {
        this.planTimeUnit = planTimeUnit;
    }
}
