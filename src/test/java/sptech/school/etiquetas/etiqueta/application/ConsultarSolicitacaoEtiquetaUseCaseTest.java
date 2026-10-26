package sptech.school.etiquetas.etiqueta.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiqueta;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.StatusSolicitacaoEtiqueta;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ConsultarSolicitacaoEtiquetaUseCaseTest {

    private static final String SOLICITACAO_ID = "6f1c2d0e-3b5a-4c7d-9e8f-0a1b2c3d4e5f";

    @Mock
    private SolicitacaoEtiquetaGateway solicitacaoEtiquetaGateway;

    @Test
    @DisplayName("Deve retornar a solicitacao registrada pelo consumer")
    void deveRetornarSolicitacao() {
        SolicitacaoEtiqueta solicitacao = new SolicitacaoEtiqueta(
                SOLICITACAO_ID, 1, StatusSolicitacaoEtiqueta.CONCLUIDA, null, 1,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
        Mockito.when(solicitacaoEtiquetaGateway.buscarPorId(SOLICITACAO_ID)).thenReturn(Optional.of(solicitacao));

        SolicitacaoEtiqueta resultado = new ConsultarSolicitacaoEtiquetaUseCase(solicitacaoEtiquetaGateway).execute(SOLICITACAO_ID);

        Assertions.assertSame(solicitacao, resultado);
    }

    @Test
    @DisplayName("Deve responder 404 quando a solicitacao nao existe")
    void deveLancar404QuandoNaoExiste() {
        Mockito.when(solicitacaoEtiquetaGateway.buscarPorId(SOLICITACAO_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> new ConsultarSolicitacaoEtiquetaUseCase(solicitacaoEtiquetaGateway).execute(SOLICITACAO_ID)
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
