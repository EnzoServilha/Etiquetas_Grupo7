package sptech.school.etiquetas.etiqueta.infrastructure.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.listener.ListenerExecutionFailedException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.boot.retry.RetryPolicySettings;
import org.springframework.core.retry.RetryPolicy;
import sptech.school.etiquetas.etiqueta.domain.FalhaEnvioEmailEtiquetaException;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaInvalidaException;

class RabbitEtiquetaConfigTest {

    private final RabbitEtiquetaConfig config = new RabbitEtiquetaConfig();

    @Test
    @DisplayName("A fila principal deve ser duravel e manter a declaracao original (sem argumentos), compativel com a fila ja existente no broker")
    void filaPrincipalDeveSerDuravelESemArgumentos() {
        Queue fila = config.etiquetaQueue();

        Assertions.assertEquals("queue_etiqueta", fila.getName());
        Assertions.assertTrue(fila.isDurable());
        Assertions.assertFalse(fila.isExclusive());
        Assertions.assertFalse(fila.isAutoDelete());
        Assertions.assertTrue(fila.getArguments().isEmpty());
    }

    @Test
    @DisplayName("A exchange, a routing key e a DLQ devem ter os nomes do contrato com o producer")
    void topologiaDeveSeguirContratoComProducer() {
        Binding binding = config.etiquetaBinding(config.etiquetaQueue(), config.etiquetaExchange());
        Binding bindingDlq = config.etiquetaDeadLetterBinding(config.etiquetaDeadLetterQueue(), config.etiquetaDeadLetterExchange());

        Assertions.assertEquals("exchange_etiqueta", binding.getExchange());
        Assertions.assertEquals("routing_key_etiqueta", binding.getRoutingKey());
        Assertions.assertEquals("queue_etiqueta", binding.getDestination());
        Assertions.assertEquals("exchange_etiqueta_dlx", bindingDlq.getExchange());
        Assertions.assertEquals("routing_key_etiqueta_dlq", bindingDlq.getRoutingKey());
        Assertions.assertEquals("queue_etiqueta_dlq", bindingDlq.getDestination());
        Assertions.assertTrue(config.etiquetaDeadLetterQueue().isDurable());
    }

    @Test
    @DisplayName("Mensagens invalidas ou malformadas nao devem ser retentadas; falhas de PDF/SMTP/banco devem")
    void politicaDeRetryDeveIgnorarMensagensInvalidas() {
        RetryPolicySettings settings = new RetryPolicySettings();
        config.etiquetaRetrySettingsCustomizer().customize(settings);
        RetryPolicy politica = settings.createRetryPolicy();
        Message mensagem = MessageBuilder.withBody("{}".getBytes()).build();

        Assertions.assertFalse(politica.shouldRetry(
                new ListenerExecutionFailedException("falhou", new SolicitacaoEtiquetaInvalidaException("invalida"), mensagem)));
        Assertions.assertFalse(politica.shouldRetry(
                new ListenerExecutionFailedException("falhou", new MessageConversionException("json invalido"), mensagem)));
        Assertions.assertTrue(politica.shouldRetry(
                new ListenerExecutionFailedException("falhou", new FalhaEnvioEmailEtiquetaException("smtp", null), mensagem)));
        Assertions.assertTrue(politica.shouldRetry(
                new ListenerExecutionFailedException("falhou", new IllegalStateException("banco"), mensagem)));
    }

    @Test
    @DisplayName("Esgotadas as tentativas, a mensagem original deve ser republicada na DLQ com o motivo da falha")
    void recovererDeveRepublicarNaDlq() {
        RabbitTemplate template = Mockito.mock(RabbitTemplate.class);
        MessageRecoverer recoverer = config.etiquetaMessageRecoverer(template);
        Message mensagem = MessageBuilder.withBody("{\"itemId\":1}".getBytes())
                .setMessageId("abc")
                .setReceivedExchange("exchange_etiqueta")
                .setReceivedRoutingKey("routing_key_etiqueta")
                .build();
        ListenerExecutionFailedException falha = new ListenerExecutionFailedException(
                "falhou", new IllegalStateException("smtp fora do ar"), mensagem);

        Assertions.assertDoesNotThrow(() -> recoverer.recover(mensagem, falha));

        ArgumentCaptor<Message> enviada = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(template).send(
                Mockito.eq("exchange_etiqueta_dlx"), Mockito.eq("routing_key_etiqueta_dlq"), enviada.capture());
        Message naDlq = enviada.getValue();
        Assertions.assertEquals("{\"itemId\":1}", new String(naDlq.getBody()));
        Assertions.assertEquals("abc", naDlq.getMessageProperties().getMessageId());
        Assertions.assertEquals(MessageDeliveryMode.PERSISTENT, naDlq.getMessageProperties().getDeliveryMode());
        Assertions.assertEquals("smtp fora do ar", naDlq.getMessageProperties().getHeaders().get("x-exception-message"));
        Assertions.assertEquals("exchange_etiqueta", naDlq.getMessageProperties().getHeaders().get("x-original-exchange"));
        Assertions.assertEquals("routing_key_etiqueta", naDlq.getMessageProperties().getHeaders().get("x-original-routingKey"));
    }

    @Test
    @DisplayName("Se a republicacao na DLQ falhar, o erro deve ser propagado para a mensagem nao ser confirmada")
    void recovererDevePropagarFalhaAoRepublicar() {
        RabbitTemplate template = Mockito.mock(RabbitTemplate.class);
        Mockito.doThrow(new org.springframework.amqp.AmqpConnectException(new java.io.IOException("broker fora")))
                .when(template).send(Mockito.anyString(), Mockito.anyString(), Mockito.any(Message.class));
        MessageRecoverer recoverer = config.etiquetaMessageRecoverer(template);
        Message mensagem = MessageBuilder.withBody("{}".getBytes()).setMessageId("abc").build();

        Assertions.assertThrows(
                org.springframework.amqp.AmqpException.class,
                () -> recoverer.recover(mensagem, new IllegalStateException("smtp")));
    }
}
