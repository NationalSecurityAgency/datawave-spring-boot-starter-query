package datawave.microservice.query.messaging.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import datawave.microservice.query.messaging.ClaimCheck;
import datawave.microservice.query.messaging.QueryResultsPublisher;
import datawave.microservice.query.messaging.Result;
import datawave.microservice.query.messaging.config.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static datawave.microservice.query.messaging.rabbitmq.RabbitMQQueryResultsManager.QUERY_RESULTS_EXCHANGE;

class RabbitMQQueryResultsPublisher implements QueryResultsPublisher {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private static final long DEFAULT_MAX_MSG_SIZE = 536870912L;
    
    private final MessagingProperties.RabbitMQProperties rabbitMQProperties;
    private final RabbitTemplate rabbitTemplate;
    private final ClaimCheck claimCheck;
    private final String queryId;
    
    private final Map<String,CountDownLatch> latchMap = new HashMap<>();
    private final Map<String,Boolean> ackMap = new HashMap<>();
    
    public RabbitMQQueryResultsPublisher(MessagingProperties.RabbitMQProperties rabbitMQProperties, RabbitTemplate rabbitTemplate, ClaimCheck claimCheck,
                    String queryId) {
        this.rabbitMQProperties = rabbitMQProperties;
        this.rabbitTemplate = rabbitTemplate;
        this.claimCheck = claimCheck;
        this.queryId = queryId;
        
        this.rabbitTemplate.setExchange(QUERY_RESULTS_EXCHANGE);
        this.rabbitTemplate.setRoutingKey(queryId);
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData != null) {
                ackMap.put(correlationData.getId(), ack);
                latchMap.get(correlationData.getId()).countDown();
            }
        });
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }
    
    @Override
    public boolean publish(Result result, long interval, TimeUnit timeUnit) {
        if (log.isDebugEnabled()) {
            log.debug("Publishing message to " + rabbitTemplate.getExchange());
        }
        
        result = resultClaimCheck(result);
        
        boolean success = false;
        if (result != null) {
            final CountDownLatch latch = new CountDownLatch(1);
            latchMap.put(result.getId(), latch);
            
            try {
                rabbitTemplate.correlationConvertAndSend(result, new CorrelationData(result.getId()));
            } catch (Exception e) {
                throw new RuntimeException("Unable to serialize results", e);
            }
            
            try {
                if (latch.await(interval, timeUnit) && ackMap.get(result.getId())) {
                    if (log.isTraceEnabled()) {
                        log.trace("Received RabbitMQ producer confirm ack for {}", result.getId());
                        success = true;
                    }
                } else {
                    log.error("Timed out while waiting for RabbitMQ producer confirm");
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for RabbitMQ producer confirm", e);
            } finally {
                latchMap.remove(result.getId());
                ackMap.remove(result.getId());
            }
        }
        
        return success;
    }
    
    private Result resultClaimCheck(Result result) {
        Result finalResult = null;
        try {
            Object payload = result.getPayload();
            byte[] resultBytes = this.rabbitTemplate.getMessageConverter().toMessage(result, new MessageProperties()).getBody();
            
            // if the message size exceeds our limit, check the payload and reencode
            long maxMessageSize = Math.min(rabbitMQProperties.getMaxMessageSizeBytes(), DEFAULT_MAX_MSG_SIZE);
            if (resultBytes.length > maxMessageSize) {
                if (claimCheck != null) {
                    finalResult = new Result(result.getId(), null);
                    claimCheck.check(queryId, payload);
                } else {
                    log.error("Result size {} exceeds max message size {} but no claim check is configured", resultBytes.length, maxMessageSize);
                }
            } else {
                finalResult = result;
            }
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize result", e);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for large payload to be checked", e);
        }
        
        return finalResult;
    }
}
