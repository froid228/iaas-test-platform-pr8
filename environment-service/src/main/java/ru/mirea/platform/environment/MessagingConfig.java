package ru.mirea.platform.environment;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    public static final String EXCHANGE = "platform.events";
    public static final String QUEUE = "environment.run-created";
    public static final String ROUTING_KEY = "run.created";
    @Bean DirectExchange platformEvents() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean Queue runCreatedQueue() { return new Queue(QUEUE, true); }
    @Bean Binding runCreatedBinding(Queue runCreatedQueue, DirectExchange platformEvents) {
        return BindingBuilder.bind(runCreatedQueue).to(platformEvents).with(ROUTING_KEY);
    }
}
