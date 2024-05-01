package datawave.microservice.query.storage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaxxer.sparsebits.SparseBitSet;

import datawave.core.query.logic.QueryKey;
import datawave.util.StringUtils;

public class TaskStates implements Serializable {
    /**
     * The possible task states: <br>
     * READY: ready to run <br>
     * RUNNING: currently running <br>
     * COMPLETED: completed successfully <br>
     * FAILED: failed to execute successfully <br>
     * ORPHANED: orphaned after the query is closed or cancelled.
     */
    public enum TASK_STATE implements Serializable {
        READY, RUNNING, COMPLETED, FAILED
    }
    
    private QueryKey queryKey;
    private int maxRunning = 1;
    private int nextTaskId = 1;
    
    @JsonIgnore
    private Map<TASK_STATE,SparseBitSet> taskStates = new HashMap<>();
    
    public TaskStates() {}
    
    public TaskStates(QueryKey queryKey, int maxRunning) {
        setQueryKey(queryKey);
        setMaxRunning(maxRunning);
    }
    
    public void setQueryKey(QueryKey key) {
        this.queryKey = key;
    }
    
    public QueryKey getQueryKey() {
        return queryKey;
    }
    
    public int getNextTaskId() {
        return nextTaskId;
    }
    
    @JsonIgnore
    public int getAndIncrementNextTaskId() {
        int taskId = nextTaskId;
        nextTaskId++;
        return taskId;
    }
    
    /**
     * Get task states in a form that is JSON serializable
     * 
     * @return taskStates
     */
    @JsonProperty("taskStates")
    public Map<TASK_STATE,String> getTaskStatesAsStrings() {
        return taskStates.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> bitSetToString(e.getValue())));
    }
    
    /**
     * Set task states in a form that was JSON serializable
     * 
     * @param taskStatesStrings
     */
    @JsonProperty("taskStates")
    public void setTaskStatesAsStrings(Map<TASK_STATE,String> taskStatesStrings) {
        taskStates = taskStatesStrings.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> stringToBitSet(e.getValue())));
    }
    
    private String bitSetToString(SparseBitSet bitSet) {
        StringBuilder builder = new StringBuilder();
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            builder.append(Integer.toString(i)).append(',');
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }
    
    private SparseBitSet stringToBitSet(String bitSetStr) {
        SparseBitSet bitSet = new SparseBitSet();
        for (String taskId : StringUtils.splitIterable(bitSetStr, ',')) {
            bitSet.set(Integer.parseInt(taskId));
        }
        return bitSet;
    }
    
    public void setNextTaskId(int nextTaskId) {
        this.nextTaskId = nextTaskId;
    }
    
    public int getMaxRunning() {
        return maxRunning;
    }
    
    public void setMaxRunning(int maxRunning) {
        this.maxRunning = maxRunning;
    }
    
    /**
     * This will get the number of tasks we can start running now (concurrently) by subtracting the number of runnning tasks from the max concurrent running
     */
    @JsonIgnore
    public int getAvailableRunningSlots() {
        return getMaxRunning() - getRunningTaskCount();
    }
    
    /**
     * This will get the number of tasks we can start running now (@see getAvailableRunningSlots) out of the tasks that are currently in a READY state. This
     * would be a minimum of the ready tasks and the available running slots
     * 
     * @return
     */
    @JsonIgnore
    public int getAvailableReadyTasksToRun() {
        return Math.min(getAvailableRunningSlots(), getReadyTaskCount());
    }
    
    public Map<TASK_STATE,SparseBitSet> getTaskStates() {
        return taskStates;
    }
    
    public void setTaskStates(Map<TASK_STATE,SparseBitSet> taskStates) {
        this.taskStates = taskStates;
    }
    
    public TASK_STATE getState(int taskId) {
        for (TASK_STATE state : TASK_STATE.values()) {
            if (taskStates.containsKey(state) && taskStates.get(state).get(taskId)) {
                return state;
            }
        }
        return null;
    }
    
    public boolean setState(int taskId, TASK_STATE taskState) {
        TASK_STATE currentState = getState(taskId);
        if (currentState == taskState) {
            return true;
        }
        if (taskState == TASK_STATE.RUNNING) {
            // if we already have the max number of running tasks, then we cannot change state
            if (getAvailableRunningSlots() <= 0) {
                return false;
            }
        }
        if (currentState != null) {
            taskStates.get(currentState).clear(taskId);
        }
        if (taskState != null) {
            if (taskStates.get(taskState) == null) {
                taskStates.put(taskState, new SparseBitSet());
            }
            taskStates.get(taskState).set(taskId);
        }
        return true;
    }
    
    public int getTaskCountForState(TASK_STATE state) {
        return taskStates.containsKey(state) ? taskStates.get(state).cardinality() : 0;
    }
    
    @JsonIgnore
    public int getReadyTaskCount() {
        return getTaskCountForState(TASK_STATE.READY);
    }
    
    @JsonIgnore
    public int getRunningTaskCount() {
        return getTaskCountForState(TASK_STATE.RUNNING);
    }
    
    @JsonIgnore
    public int getFailedTaskCount() {
        return getTaskCountForState(TASK_STATE.FAILED);
    }
    
    @JsonIgnore
    public int getCompletedTaskCount() {
        return getTaskCountForState(TASK_STATE.COMPLETED);
    }
    
    public boolean hasTasksForState(TASK_STATE state) {
        return taskStates.containsKey(state) && !taskStates.get(state).isEmpty();
    }
    
    @JsonIgnore
    public boolean hasReadyTasks() {
        return hasTasksForState(TASK_STATE.READY);
    }
    
    @JsonIgnore
    public boolean hasRunningTasks() {
        return hasTasksForState(TASK_STATE.RUNNING);
    }
    
    @JsonIgnore
    public boolean hasUnfinishedTasks() {
        return hasReadyTasks() || hasRunningTasks();
    }
    
    @JsonIgnore
    public boolean hasCompletedTasks() {
        return hasTasksForState(TASK_STATE.COMPLETED);
    }
    
    @JsonIgnore
    public boolean hasFailedTasks() {
        return hasTasksForState(TASK_STATE.FAILED);
    }
    
    public Iterable<TaskKey> getTasksForState(TASK_STATE state, int maxTasks) {
        return new Iterable<TaskKey>() {
            private SparseBitSet states = (taskStates.containsKey(state) ? taskStates.get(state) : new SparseBitSet());
            
            @Override
            public Iterator<TaskKey> iterator() {
                return new Iterator<TaskKey>() {
                    private int nextTaskId = states.nextSetBit(0);
                    private int count = 0;
                    
                    @Override
                    public boolean hasNext() {
                        return (count != maxTasks) && (nextTaskId >= 0);
                    }
                    
                    @Override
                    public TaskKey next() {
                        if (hasNext()) {
                            TaskKey returnTask = new TaskKey(nextTaskId, queryKey);
                            nextTaskId = states.nextSetBit(nextTaskId + 1);
                            count++;
                            return returnTask;
                        }
                        return null;
                    }
                };
            }
        };
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(queryKey).append(maxRunning).append(taskStates).build();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TaskStates) {
            TaskStates other = (TaskStates) obj;
            return new EqualsBuilder().append(queryKey, other.queryKey).append(maxRunning, other.maxRunning).append(taskStates, other.taskStates).build();
        }
        return false;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this).append("queryKey", queryKey).append("maxRunning", maxRunning).append("taskStates", taskStatesString()).build();
    }
    
    public String taskStatesString() {
        StringBuilder str = new StringBuilder();
        String sep = "";
        for (TASK_STATE state : TASK_STATE.values()) {
            str.append(sep);
            str.append(state).append(':').append(getTaskCountForState(state));
            sep = ",";
        }
        return str.toString();
    }
}
