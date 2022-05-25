package datawave.microservice.query.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import datawave.microservice.query.messaging.QueryResultsPublisher;
import datawave.microservice.query.messaging.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class KafkaQueryResultsPublisher implements QueryResultsPublisher {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final KafkaTemplate<String,String> kafkaTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public KafkaQueryResultsPublisher(KafkaTemplate<String,String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Override
    public boolean publish(Result result, long interval, TimeUnit timeUnit) {
        if (log.isDebugEnabled()) {
            log.debug("Publishing message to " + kafkaTemplate.getDefaultTopic());
        }
        
        boolean success = false;
        try {
            // @formatter:off
            kafkaTemplate
                    .send(MessageBuilder.withPayload(objectMapper.writeValueAsString(result)).build())
                    .get(interval, timeUnit);
            // @formatter:on
            success = true;
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize result", e);
        } catch (TimeoutException e) {
            log.error("Timed out waiting for kafka send result", e);
        } catch (InterruptedException e) {
            log.error("Interrupted waiting for kafka send result", e);
        } catch (ExecutionException e) {
            log.error("Execution exception waiting for kafka send result", e);
        }
        return success;
    }
}
