package ru.mirea.platform.environment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RunCreatedListener {
    private static final Logger log = LoggerFactory.getLogger(RunCreatedListener.class);
    private final ObjectMapper mapper;
    public RunCreatedListener(ObjectMapper mapper) { this.mapper = mapper; }
    @RabbitListener(queues = MessagingConfig.QUEUE)
    public void receive(String body) {
        try {
            var event = mapper.readTree(body);
            log.info("requestId={} event=run.created eventId={} runId={} environmentId={} consumed queue={}",
                    event.path("requestId").asText(), event.path("eventId").asText(),
                    event.path("runId").asText(), event.path("environmentId").asText(), MessagingConfig.QUEUE);
        } catch (JsonProcessingException ex) {
            throw new AmqpRejectAndDontRequeueException("Invalid run.created JSON", ex);
        }
    }
}
