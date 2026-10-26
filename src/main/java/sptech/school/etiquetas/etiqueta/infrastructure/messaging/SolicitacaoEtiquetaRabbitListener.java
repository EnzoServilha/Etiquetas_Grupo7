package sptech.school.etiquetas.etiqueta.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import sptech.school.etiquetas.etiqueta.application.ProcessarSolicitacaoEtiquetaUseCase;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaMessage;
import sptech.school.etiquetas.etiqueta.infrastructure.config.RabbitEtiquetaConfig;

@Component
public class SolicitacaoEtiquetaRabbitListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolicitacaoEtiquetaRabbitListener.class);

    private final ProcessarSolicitacaoEtiquetaUseCase processarSolicitacaoEtiquetaUseCase;

    public SolicitacaoEtiquetaRabbitListener(ProcessarSolicitacaoEtiquetaUseCase processarSolicitacaoEtiquetaUseCase) {
        this.processarSolicitacaoEtiquetaUseCase = processarSolicitacaoEtiquetaUseCase;
    }

    /**
     * Ack automático: a mensagem só é confirmada quando este método retorna sem erro. Se lançar exceção, a política de
     * retry (RabbitEtiquetaConfig) decide entre nova tentativa e envio para a DLQ.
     */
    @RabbitListener(queues = RabbitEtiquetaConfig.QUEUE_ETIQUETA, messageConverter = "jsonConverter")
    public void consumir(SolicitacaoEtiquetaMessage message) {
        String solicitacaoId = message == null ? null : message.solicitacaoId();
        Integer itemId = message == null ? null : message.itemId();
        LOGGER.info("Solicitacao de etiqueta recebida: solicitacaoId={}, itemId={}.", solicitacaoId, itemId);
        long inicio = System.nanoTime();

        try {
            ProcessarSolicitacaoEtiquetaUseCase.Resultado resultado = processarSolicitacaoEtiquetaUseCase.execute(message);
            long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;

            if (resultado == ProcessarSolicitacaoEtiquetaUseCase.Resultado.JA_CONCLUIDA) {
                LOGGER.info("Solicitacao de etiqueta {} ja havia sido concluida; reentrega duplicada ignorada.", solicitacaoId);
            } else {
                LOGGER.info("Solicitacao de etiqueta concluida: solicitacaoId={}, itemId={}, duracaoMs={}.",
                        solicitacaoId, itemId, duracaoMs);
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("Falha ao processar solicitacao de etiqueta: solicitacaoId={}, itemId={}, causa={}.",
                    solicitacaoId, itemId, ex.toString());
            throw ex;
        }
    }
}
