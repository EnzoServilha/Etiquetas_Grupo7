package sptech.school.etiquetas.etiqueta.infrastructure.messaging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import sptech.school.etiquetas.etiqueta.application.ProcessarSolicitacaoEtiquetaUseCase;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaInvalidaException;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaMessage;

@ExtendWith(MockitoExtension.class)
class SolicitacaoEtiquetaRabbitListenerTest {

    private static final SolicitacaoEtiquetaMessage MESSAGE = new SolicitacaoEtiquetaMessage(
            "6f1c2d0e-3b5a-4c7d-9e8f-0a1b2c3d4e5f", 1, "ABC-123", "Bosch", "Filtro", "2026-09-03T23:00");

    @Mock
    private ProcessarSolicitacaoEtiquetaUseCase processarSolicitacaoEtiquetaUseCase;

    @Test
    @DisplayName("Deve retornar normalmente (ack) quando a solicitacao e processada")
    void deveConfirmarQuandoProcessada() {
        Mockito.when(processarSolicitacaoEtiquetaUseCase.execute(MESSAGE))
                .thenReturn(ProcessarSolicitacaoEtiquetaUseCase.Resultado.PROCESSADA);

        Assertions.assertDoesNotThrow(() -> new SolicitacaoEtiquetaRabbitListener(processarSolicitacaoEtiquetaUseCase).consumir(MESSAGE));

        Mockito.verify(processarSolicitacaoEtiquetaUseCase).execute(MESSAGE);
    }

    @Test
    @DisplayName("Deve retornar normalmente (ack) quando a mensagem e uma reentrega de solicitacao ja concluida")
    void deveConfirmarQuandoJaConcluida() {
        Mockito.when(processarSolicitacaoEtiquetaUseCase.execute(MESSAGE))
                .thenReturn(ProcessarSolicitacaoEtiquetaUseCase.Resultado.JA_CONCLUIDA);

        Assertions.assertDoesNotThrow(() -> new SolicitacaoEtiquetaRabbitListener(processarSolicitacaoEtiquetaUseCase).consumir(MESSAGE));
    }

    @Test
    @DisplayName("Deve propagar a excecao (sem ack) para a politica de retry/DLQ quando o processamento falha")
    void devePropagarExcecaoQuandoFalha() {
        IllegalStateException falha = new IllegalStateException("SMTP indisponivel");
        Mockito.when(processarSolicitacaoEtiquetaUseCase.execute(MESSAGE)).thenThrow(falha);

        IllegalStateException lancada = Assertions.assertThrows(
                IllegalStateException.class,
                () -> new SolicitacaoEtiquetaRabbitListener(processarSolicitacaoEtiquetaUseCase).consumir(MESSAGE)
        );

        Assertions.assertSame(falha, lancada);
    }

    @Test
    @DisplayName("Deve propagar a excecao de mensagem invalida mesmo quando o corpo da mensagem e nulo")
    void devePropagarMensagemInvalidaComCorpoNulo() {
        Mockito.when(processarSolicitacaoEtiquetaUseCase.execute(null))
                .thenThrow(new SolicitacaoEtiquetaInvalidaException("Mensagem vazia"));

        Assertions.assertThrows(
                SolicitacaoEtiquetaInvalidaException.class,
                () -> new SolicitacaoEtiquetaRabbitListener(processarSolicitacaoEtiquetaUseCase).consumir(null)
        );
    }
}
