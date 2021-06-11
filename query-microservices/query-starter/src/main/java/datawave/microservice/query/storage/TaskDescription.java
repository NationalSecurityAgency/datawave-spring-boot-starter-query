package datawave.microservice.query.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import datawave.microservice.query.remote.QueryRequest;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A task description
 */
@XmlRootElement
public class TaskDescription {
    private final TaskKey taskKey;
    private final Map<String,String> parameters;
    
    @JsonCreator
    public TaskDescription(@JsonProperty("taskKey") TaskKey taskKey, @JsonProperty("parameters") Map<String,String> parameters) {
        this.taskKey = taskKey;
        this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
    }
    
    public TaskKey getTaskKey() {
        return taskKey;
    }
    
    public Map<String,String> getParameters() {
        return parameters;
    }
    
    @Override
    public String toString() {
        return getTaskKey() + " on " + getParameters();
    }
    
    public String toDebug() {
        return getTaskKey().toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof TaskDescription) {
            TaskDescription other = (TaskDescription) o;
            return new EqualsBuilder().append(getTaskKey(), other.getTaskKey()).append(getParameters(), other.getParameters()).isEquals();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getTaskKey()).append(getParameters()).toHashCode();
    }
}
