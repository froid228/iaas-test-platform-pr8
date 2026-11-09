package ru.mirea.platform.testrun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RunEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(RunEventPublisher.class);
    private final RabbitTemplate rabbit;
    private final ObjectMapper mapper;
    public RunEventPublisher(RabbitTemplate rabbit, ObjectMapper mapper) {
        this.rabbit = rabbit; this.mapper = mapper;
    }
    @TransactionalEventListener
    public void publish(RunCreated event) {
        try {
            rabbit.convertAndSend(MessagingConfig.EXCHANGE, MessagingConfig.ROUTING_KEY,
                    mapper.writeValueAsString(event), message -> {
                        message.getMessageProperties().setContentType("application/json");
                        message.getMessageProperties().setHeader("X-Request-Id", event.requestId());
                        message.getMessageProperties().setMessageId(event.eventId().toString());
                        return message;
                    });
            log.info("requestId={} event=run.created eventId={} runId={} exchange={} published",
                    event.requestId(), event.eventId(), event.runId(), MessagingConfig.EXCHANGE);
        } catch (AmqpException | JsonProcessingException ex) {
            // Учебное уведомление: запись уже сохранена; повторная доставка требует outbox.
            log.error("requestId={} event=run.created eventId={} runId={} publication_failed type={}",
                    event.requestId(), event.eventId(), event.runId(), ex.getClass().getSimpleName());
        }
    }
}
