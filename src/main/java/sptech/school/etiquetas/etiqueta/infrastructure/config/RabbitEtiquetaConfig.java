package sptech.school.etiquetas.etiqueta.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaInvalidaException;

/**
 * Topologia da fila de etiquetas. O producer (BackEnd_Grupo7) declara a mesma exchange, fila e binding principais;
 * qualquer diferença nos argumentos da fila causa PRECONDITION_FAILED no broker.
 *
 * <p>A fila principal mantém a declaração original (durável, sem argumentos), pois o RabbitMQ não permite alterar os
 * argumentos de uma fila já criada. A DLQ é de responsabilidade exclusiva deste consumer.</p>
 */
@Configuration
public class RabbitEtiquetaConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitEtiquetaConfig.class);

    public static final String EXCHANGE_ETIQUETA = "exchange_etiqueta";
    public static final String QUEUE_ETIQUETA = "queue_etiqueta";
    public static final String ROUTING_KEY_ETIQUETA = "routing_key_etiqueta";

    public static final String EXCHANGE_ETIQUETA_DLX = "exchange_etiqueta_dlx";
    public static final String QUEUE_ETIQUETA_DLQ = "queue_etiqueta_dlq";
    public static final String ROUTING_KEY_ETIQUETA_DLQ = "routing_key_etiqueta_dlq";

    @Bean
    public DirectExchange etiquetaExchange() {
        return new DirectExchange(EXCHANGE_ETIQUETA);
    }

    @Bean
    public Queue etiquetaQueue() {
        return QueueBuilder
                .durable(QUEUE_ETIQUETA)
                .build();
    }

    @Bean
    public Binding etiquetaBinding(
            Queue etiquetaQueue,
            DirectExchange etiquetaExchange
    ) {
        return BindingBuilder
                .bind(etiquetaQueue)
                .to(etiquetaExchange)
                .with(ROUTING_KEY_ETIQUETA);
    }

    @Bean
    public DirectExchange etiquetaDeadLetterExchange() {
        return new DirectExchange(EXCHANGE_ETIQUETA_DLX);
    }

    @Bean
    public Queue etiquetaDeadLetterQueue() {
        return QueueBuilder
                .durable(QUEUE_ETIQUETA_DLQ)
                .build();
    }

    @Bean
    public Binding etiquetaDeadLetterBinding(
            Queue etiquetaDeadLetterQueue,
            DirectExchange etiquetaDeadLetterExchange
    ) {
        return BindingBuilder
                .bind(etiquetaDeadLetterQueue)
                .to(etiquetaDeadLetterExchange)
                .with(ROUTING_KEY_ETIQUETA_DLQ);
    }

    @Bean
    public JacksonJsonMessageConverter jsonConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Mensagens malformadas ou inválidas (falha de conversão do JSON ou SolicitacaoEtiquetaInvalidaException)
     * não são retentadas: reprocessar não muda o resultado. As demais falhas (PDF, SMTP, banco) seguem a política
     * spring.rabbitmq.listener.simple.retry.*.
     */
    @Bean
    public RabbitListenerRetrySettingsCustomizer etiquetaRetrySettingsCustomizer() {
        ConditionalRejectingErrorHandler.DefaultExceptionStrategy mensagemInvalida =
                new ConditionalRejectingErrorHandler.DefaultExceptionStrategy() {
                    @Override
                    protected boolean isUserCauseFatal(Throwable cause) {
                        return cause instanceof SolicitacaoEtiquetaInvalidaException;
                    }
                };
        return settings -> settings.setExceptionPredicate(throwable -> !mensagemInvalida.isFatal(throwable));
    }

    /**
     * Executado quando as tentativas se esgotam (ou a falha não é retentável): republica a mensagem original na DLQ,
     * com os cabeçalhos x-exception-message, x-original-exchange e x-original-routingKey, e em seguida a mensagem é
     * confirmada (ack) na fila principal. O stack trace completo fica no log de erro.
     */
    @Bean
    public MessageRecoverer etiquetaMessageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, EXCHANGE_ETIQUETA_DLX, ROUTING_KEY_ETIQUETA_DLQ) {
            @Override
            public void recover(Message message, Throwable cause) {
                LOGGER.error("Solicitacao de etiqueta (messageId={}) nao pode ser processada e sera enviada para a DLQ '{}'.",
                        message.getMessageProperties().getMessageId(), QUEUE_ETIQUETA_DLQ, cause);
                super.recover(message, cause);
            }
        };
    }
}
