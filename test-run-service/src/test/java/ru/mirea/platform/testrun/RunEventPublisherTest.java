package ru.mirea.platform.testrun;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import static org.mockito.Mockito.*;

class RunEventPublisherTest {
    @Test void publishesOnlyAfterSuccessfulCommit() {
        verifyTransaction(false);
    }
    @Test void rollbackDoesNotPublishEvent() {
        verifyTransaction(true);
    }
    private void verifyTransaction(boolean rollback) {
        var rabbit = mock(RabbitTemplate.class);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(RabbitTemplate.class, () -> rabbit);
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(RunEventPublisher.class);
            context.refresh();
            clearInvocations(rabbit); // Spring lifecycle callbacks are unrelated to message delivery.
            var transaction = new TransactionTemplate(new TestTransactionManager());
            transaction.executeWithoutResult(status -> {
                context.publishEvent(new RunCreated(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "test-request"));
                verifyNoInteractions(rabbit);
                if (rollback) status.setRollbackOnly();
            });
            if (rollback) verifyNoInteractions(rabbit);
            else verify(rabbit).convertAndSend(eq(MessagingConfig.EXCHANGE), eq(MessagingConfig.ROUTING_KEY),
                    anyString(), any(MessagePostProcessor.class));
        }
    }
    static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
        @Override protected void doCommit(DefaultTransactionStatus status) {}
        @Override protected void doRollback(DefaultTransactionStatus status) {}
    }
}
