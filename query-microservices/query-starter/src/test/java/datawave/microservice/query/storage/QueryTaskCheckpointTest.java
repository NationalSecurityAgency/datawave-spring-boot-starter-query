package datawave.microservice.query.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import datawave.microservice.query.logic.QueryCheckpoint;
import datawave.microservice.query.logic.QueryKey;
import datawave.microservice.query.logic.QueryPool;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class QueryTaskCheckpointTest {
    @Test
    public void testQueryPool() {
        QueryPool queryPool = new QueryPool("default");
        assertEquals("default", queryPool.getName());
        
        QueryPool queryPool2 = new QueryPool("default");
        assertEquals(queryPool, queryPool2);
        assertEquals(queryPool.hashCode(), queryPool2.hashCode());
        
        QueryPool otherPool = new QueryPool("other");
        assertNotEquals(otherPool, queryPool);
    }
    
    @Test
    public void testQueryKey() {
        UUID queryId = UUID.randomUUID();
        QueryPool queryPool = new QueryPool("default");
        String queryLogic = "EventQuery";
        QueryKey key = new QueryKey(queryPool, queryId, queryLogic);
        assertEquals(queryId, key.getQueryId());
        assertEquals(queryPool, key.getQueryPool());
        
        UUID queryId2 = UUID.fromString(queryId.toString());
        QueryPool queryPool2 = new QueryPool("default");
        String queryLogic2 = "EventQuery";
        QueryKey key2 = new QueryKey(queryPool2, queryId2, queryLogic2);
        assertEquals(key, key2);
        assertEquals(key.hashCode(), key2.hashCode());
        assertEquals(key.toKey(), key2.toKey());
        
        assertTrue(key.toKey().contains(queryId.toString()));
        assertTrue(key.toKey().contains(queryPool.toString()));
        assertTrue(key.toKey().contains(queryLogic));
        
        UUID otherId = UUID.randomUUID();
        QueryKey otherKey = new QueryKey(queryPool, otherId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        
        QueryPool otherPool = new QueryPool("other");
        otherKey = new QueryKey(otherPool, queryId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        
        String otherLogic = "EdgeQuery";
        otherKey = new QueryKey(queryPool, queryId, otherLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
    }
    
    @Test
    public void getTaskKey() {
        UUID queryId = UUID.randomUUID();
        QueryPool queryPool = new QueryPool("default");
        String queryLogic = "EventQuery";
        UUID taskId = UUID.randomUUID();
        QueryTask.QUERY_ACTION action = QueryTask.QUERY_ACTION.CREATE;
        TaskKey key = new TaskKey(taskId, action, queryPool, queryId, queryLogic);
        assertEquals(queryId, key.getQueryId());
        assertEquals(queryPool, key.getQueryPool());
        assertEquals(taskId, key.getTaskId());
        assertEquals(action, key.getAction());
        
        UUID queryId2 = UUID.fromString(queryId.toString());
        QueryPool queryPool2 = new QueryPool("default");
        UUID taskId2 = UUID.fromString(taskId.toString());
        String queryLogic2 = "EventQuery";
        QueryTask.QUERY_ACTION action2 = QueryTask.QUERY_ACTION.CREATE;
        TaskKey key2 = new TaskKey(taskId2, action2, queryPool2, queryId2, queryLogic2);
        assertEquals(key, key2);
        assertEquals(key.hashCode(), key2.hashCode());
        assertEquals(key.toKey(), key2.toKey());
        
        assertTrue(key.toKey().contains(taskId.toString()));
        assertTrue(key.toKey().contains(action.toString()));
        assertTrue(key.toKey().contains(queryId.toString()));
        assertTrue(key.toKey().contains(queryPool.toString()));
        
        UUID otherId = UUID.randomUUID();
        QueryPool otherPool = new QueryPool("other");
        String otherLogic = "EdgeQuery";
        QueryTask.QUERY_ACTION otherAction = QueryTask.QUERY_ACTION.NEXT;
        TaskKey otherKey = new TaskKey(otherId, action, queryPool, queryId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        otherKey = new TaskKey(taskId, action, otherPool, queryId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        otherKey = new TaskKey(taskId, otherAction, queryPool, queryId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        otherKey = new TaskKey(taskId, action, queryPool, otherId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        otherKey = new TaskKey(taskId, action, queryPool, queryId, otherLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
    }
    
    @Test
    public void testQueryTaskNotification() {
        UUID queryId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        QueryPool queryPool = new QueryPool("default");
        String queryLogic = "EventQuery";
        QueryTask.QUERY_ACTION action = QueryTask.QUERY_ACTION.CREATE;
        TaskKey taskKey = new TaskKey(taskId, action, queryPool, queryId, queryLogic);
        QueryTaskNotification notification = new QueryTaskNotification(taskKey);
        
        assertEquals(taskKey, notification.getTaskKey());
        
        UUID taskId2 = UUID.fromString(taskId.toString());
        UUID queryId2 = UUID.fromString(queryId.toString());
        QueryPool queryPool2 = new QueryPool("default");
        String queryLogic2 = "EventQuery";
        QueryTask.QUERY_ACTION action2 = QueryTask.QUERY_ACTION.CREATE;
        TaskKey taskKey2 = new TaskKey(taskId2, action2, queryPool2, queryId2, queryLogic2);
        QueryTaskNotification notification2 = new QueryTaskNotification(taskKey2);
        assertEquals(notification, notification2);
        assertEquals(notification.hashCode(), notification2.hashCode());
        
        UUID otherId = UUID.randomUUID();
        TaskKey otherKey = new TaskKey(otherId, action, queryPool, queryId, queryLogic);
        QueryTaskNotification otherNotification = new QueryTaskNotification(otherKey);
        assertNotEquals(otherNotification, notification);
        otherNotification = new QueryTaskNotification(otherKey);
        assertNotEquals(otherNotification, notification);
    }
    
    @Test
    public void testCheckpoint() {
        UUID uuid = UUID.randomUUID();
        QueryPool queryPool = new QueryPool("default");
        String queryLogic = "EventQuery";
        Map<String,Object> props = new HashMap<>();
        props.put("name", "foo");
        props.put("query", "foo == bar");
        QueryCheckpoint qcp = new QueryCheckpoint(queryPool, uuid, queryLogic, props);
        
        assertEquals(queryPool, qcp.getQueryKey().getQueryPool());
        assertEquals(props, qcp.getProperties());
        assertEquals(uuid, qcp.getQueryKey().getQueryId());
        
        UUID uuid2 = UUID.fromString(uuid.toString());
        QueryPool queryPool2 = new QueryPool("default");
        String queryLogic2 = "EventQuery";
        Map<String,Object> props2 = new HashMap<>();
        props2.put("name", "foo");
        props2.put("query", "foo == bar");
        assertEquals(props, props2);
        QueryCheckpoint qcp2 = new QueryCheckpoint(queryPool2, uuid2, queryLogic2, props2);
        
        assertEquals(qcp, qcp2);
        assertEquals(qcp.hashCode(), qcp2.hashCode());
        
        UUID otherId = UUID.randomUUID();
        QueryPool otherPool = new QueryPool("other");
        String otherLogic = "EdgeQuery";
        Map<String,Object> otherProps = new HashMap<>();
        otherProps.put("name", "bar");
        QueryCheckpoint otherCp = new QueryCheckpoint(otherPool, uuid, queryLogic, props);
        assertNotEquals(otherCp, qcp);
        otherCp = new QueryCheckpoint(queryPool, otherId, queryLogic, props);
        assertNotEquals(otherCp, qcp);
        otherCp = new QueryCheckpoint(queryPool, uuid, otherLogic, props);
        assertNotEquals(otherCp, qcp);
        otherCp = new QueryCheckpoint(queryPool, uuid, queryLogic, otherProps);
        assertNotEquals(otherCp, qcp);
    }
    
    @Test
    public void testTask() {
        UUID uuid = UUID.randomUUID();
        QueryPool queryPool = new QueryPool("default");
        String queryLogic = "EventQuery";
        Map<String,Object> props = new HashMap<>();
        props.put("name", "foo");
        props.put("query", "foo == bar");
        QueryCheckpoint qcp = new QueryCheckpoint(queryPool, uuid, queryLogic, props);
        QueryTask task = new QueryTask(QueryTask.QUERY_ACTION.CREATE, qcp);
        
        assertEquals(QueryTask.QUERY_ACTION.CREATE, task.getAction());
        assertEquals(qcp, task.getQueryCheckpoint());
        
        QueryTaskNotification notification = task.getNotification();
        assertEquals(task.getTaskKey(), notification.getTaskKey());
        assertEquals(QueryTask.QUERY_ACTION.CREATE, notification.getAction());
        
        UUID uuid2 = UUID.fromString(uuid.toString());
        QueryPool queryPool2 = new QueryPool("default");
        String queryLogic2 = "EventQuery";
        Map<String,Object> props2 = new HashMap<>();
        props2.put("name", "foo");
        props2.put("query", "foo == bar");
        QueryCheckpoint qcp2 = new QueryCheckpoint(queryPool2, uuid2, queryLogic2, props2);
        assertEquals(qcp, qcp2);
        QueryTask task2 = new QueryTask(task.getTaskKey().getTaskId(), QueryTask.QUERY_ACTION.CREATE, qcp2);
        
        assertEquals(task, task2);
        assertEquals(task.hashCode(), task2.hashCode());
        assertEquals(task.getTaskKey(), task2.getTaskKey());
        
        UUID otherId = UUID.randomUUID();
        QueryCheckpoint otherCp = new QueryCheckpoint(queryPool, otherId, queryLogic, props);
        QueryTask otherTask = new QueryTask(otherId, QueryTask.QUERY_ACTION.CREATE, qcp);
        assertNotEquals(otherTask, task);
        assertNotEquals(otherTask.getTaskKey(), task.getTaskKey());
        otherTask = new QueryTask(task.getTaskKey().getTaskId(), QueryTask.QUERY_ACTION.NEXT, qcp);
        assertNotEquals(otherTask, task);
        assertNotEquals(otherTask.getTaskKey(), task.getTaskKey());
        otherTask = new QueryTask(task.getTaskKey().getTaskId(), QueryTask.QUERY_ACTION.CREATE, otherCp);
        assertNotEquals(otherTask, task);
        assertNotEquals(otherTask.getTaskKey(), task.getTaskKey());
    }
    
    @Test
    public void testTaskDescription() throws JsonProcessingException {
        TaskKey key = new TaskKey(UUID.randomUUID(), QueryTask.QUERY_ACTION.CREATE, new QueryPool("default"), UUID.randomUUID(), "EventQuery");
        Map<String,String> props = new HashMap<>();
        props.put("name", "foo");
        props.put("query", "foo == bar");
        TaskDescription desc = new TaskDescription(key, QueryTask.QUERY_ACTION.CREATE, props);
        
        assertEquals(key, desc.getTaskKey());
        assertEquals(QueryTask.QUERY_ACTION.CREATE, desc.getAction());
        assertEquals(props, desc.getParameters());
        
        String json = new ObjectMapper().writeValueAsString(desc);
        TaskDescription desc2 = new ObjectMapper().readerFor(TaskDescription.class).readValue(json);
        assertEquals(desc, desc2);
        assertEquals(desc.hashCode(), desc2.hashCode());
        
        TaskKey key2 = new TaskKey(key.getTaskId(), key.getAction(), key.getQueryPool(), key.getQueryId(), key.getQueryLogic());
        Map<String,String> props2 = new HashMap<>();
        props2.put("name", "foo");
        props2.put("query", "foo == bar");
        desc2 = new TaskDescription(key2, QueryTask.QUERY_ACTION.CREATE, props2);
        
        assertEquals(desc, desc2);
        assertEquals(desc.hashCode(), desc.hashCode());
        
        TaskKey otherKey = new TaskKey(UUID.randomUUID(), key.getAction(), key.getQueryPool(), key.getQueryId(), key.getQueryLogic());
        Map<String,String> otherProps = new HashMap<>();
        otherProps.put("name", "bar");
        otherProps.put("query", "foo == bar");
        TaskDescription otherDesc = new TaskDescription(otherKey, QueryTask.QUERY_ACTION.CREATE, props);
        assertNotEquals(otherDesc, desc);
        otherDesc = new TaskDescription(key, QueryTask.QUERY_ACTION.NEXT, props);
        assertNotEquals(otherDesc, desc);
        otherDesc = new TaskDescription(key, QueryTask.QUERY_ACTION.CREATE, otherProps);
        assertNotEquals(otherDesc, desc);
    }
    
    @Test
    public void testQueryState() throws JsonProcessingException {
        UUID uuid = UUID.randomUUID();
        QueryPool queryPool = new QueryPool("default");
        String queryLogic = "EventQuery";
        QueryStatus queryStatus = new QueryStatus(new QueryKey(queryPool, uuid, queryLogic));
        TaskStates tasks = new TaskStates(new QueryKey(queryPool, uuid, queryLogic), 10);
        Map<TaskStates.TASK_STATE,Set<TaskKey>> states = new HashMap<>();
        QueryTask.QUERY_ACTION action = QueryTask.QUERY_ACTION.CREATE;
        states.put(TaskStates.TASK_STATE.READY, new HashSet<>());
        states.get(TaskStates.TASK_STATE.READY).add(new TaskKey(UUID.randomUUID(), action, queryPool, uuid, queryLogic));
        states.get(TaskStates.TASK_STATE.READY).add(new TaskKey(UUID.randomUUID(), action, queryPool, uuid, queryLogic));
        tasks.setTaskStates(states);
        QueryState state = new QueryState(queryPool, uuid, queryLogic, queryStatus, tasks);
        
        assertEquals(uuid, state.getQueryId());
        assertEquals(queryPool, state.getQueryPool());
        assertEquals(queryLogic, state.getQueryLogic());
        assertEquals(tasks, state.getTaskStates());
        
        String json = new ObjectMapper().writeValueAsString(state);
        QueryState state2 = new ObjectMapper().readerFor(QueryState.class).readValue(json);
        assertEquals(state, state2);
        assertEquals(state.hashCode(), state2.hashCode());
        
        UUID uuid2 = UUID.fromString(uuid.toString());
        QueryPool queryPool2 = new QueryPool("default");
        String queryLogic2 = "EventQuery";
        QueryStatus queryStatus2 = new QueryStatus(new QueryKey(queryPool2, uuid2, queryLogic2));
        TaskStates tasks2 = new TaskStates(new QueryKey(queryPool, uuid, queryLogic), 10);
        tasks2.setTaskStates(new HashMap<>(states));
        state2 = new QueryState(queryPool2, uuid2, queryLogic2, queryStatus2, tasks2);
        
        assertEquals(state, state2);
        assertEquals(state.hashCode(), state2.hashCode());
        
        UUID otherId = UUID.randomUUID();
        QueryPool otherPool = new QueryPool("other");
        String otherLogic = "EdgeQuery";
        QueryStatus otherProperties = new QueryStatus(new QueryKey(otherPool, otherId, otherLogic));
        TaskStates otherTasks = new TaskStates(new QueryKey(queryPool, uuid, queryLogic), 10);
        Map<TaskStates.TASK_STATE,Set<TaskKey>> otherStates = new HashMap<>();
        otherStates.put(TaskStates.TASK_STATE.READY, new HashSet<>());
        otherStates.get(TaskStates.TASK_STATE.READY).add(new TaskKey(UUID.randomUUID(), action, queryPool, uuid, queryLogic));
        otherStates.get(TaskStates.TASK_STATE.READY).add(new TaskKey(UUID.randomUUID(), action, queryPool, uuid, queryLogic));
        otherTasks.setTaskStates(otherStates);
        QueryState otherState = new QueryState(queryPool, otherId, queryLogic, queryStatus, tasks);
        assertNotEquals(otherState, state);
        otherState = new QueryState(otherPool, uuid, queryLogic, queryStatus, tasks);
        assertNotEquals(otherState, state);
        otherState = new QueryState(queryPool, uuid, otherLogic, queryStatus, tasks);
        assertNotEquals(otherState, state);
        otherState = new QueryState(queryPool, uuid, queryLogic, otherProperties, tasks);
        assertNotEquals(otherState, state);
        otherState = new QueryState(queryPool, uuid, queryLogic, queryStatus, otherTasks);
        assertNotEquals(otherState, state);
    }
}
