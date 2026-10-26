package sptech.school.etiquetas.etiqueta.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import sptech.school.etiquetas.etiqueta.domain.EnviarEtiquetaPorEmailGateway;
import sptech.school.etiquetas.etiqueta.domain.Etiqueta;
import sptech.school.etiquetas.etiqueta.domain.FalhaEnvioEmailEtiquetaException;
import sptech.school.etiquetas.etiqueta.domain.GerarPdfEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaInvalidaException;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaMessage;

import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ProcessarSolicitacaoEtiquetaUseCaseTest {

    private static final String SOLICITACAO_ID = "6f1c2d0e-3b5a-4c7d-9e8f-0a1b2c3d4e5f";

    @Mock
    private SolicitacaoEtiquetaGateway solicitacaoEtiquetaGateway;

    @Mock
    private GerarPdfEtiquetaGateway gerarPdfEtiquetaGateway;

    @Mock
    private EnviarEtiquetaPorEmailGateway enviarEtiquetaPorEmailGateway;

    private ProcessarSolicitacaoEtiquetaUseCase criarUseCase() {
        return new ProcessarSolicitacaoEtiquetaUseCase(
                solicitacaoEtiquetaGateway,
                gerarPdfEtiquetaGateway,
                enviarEtiquetaPorEmailGateway
        );
    }

    private static SolicitacaoEtiquetaMessage mensagemValida() {
        return new SolicitacaoEtiquetaMessage(
                SOLICITACAO_ID,
                1,
                "ABC-123",
                "Bosch",
                "Filtro de oleo",
                "2026-09-03T23:00"
        );
    }

    @Test
    @DisplayName("Deve gerar o PDF, enviar por e-mail e marcar a solicitacao como concluida")
    void deveProcessarSolicitacao() {
        SolicitacaoEtiquetaMessage message = mensagemValida();
        byte[] pdf = "pdf".getBytes();
        Etiqueta etiqueta = new Etiqueta("ABC-123", "Bosch", "Filtro de oleo", "2026-09-03T23:00");
        Mockito.when(solicitacaoEtiquetaGateway.iniciarProcessamento(SOLICITACAO_ID, 1)).thenReturn(true);
        Mockito.when(gerarPdfEtiquetaGateway.gerar(etiqueta)).thenReturn(pdf);

        ProcessarSolicitacaoEtiquetaUseCase.Resultado resultado = criarUseCase().execute(message);

        Assertions.assertEquals(ProcessarSolicitacaoEtiquetaUseCase.Resultado.PROCESSADA, resultado);
        InOrder ordem = Mockito.inOrder(solicitacaoEtiquetaGateway, gerarPdfEtiquetaGateway, enviarEtiquetaPorEmailGateway);
        ordem.verify(solicitacaoEtiquetaGateway).iniciarProcessamento(SOLICITACAO_ID, 1);
        ordem.verify(gerarPdfEtiquetaGateway).gerar(etiqueta);
        ordem.verify(enviarEtiquetaPorEmailGateway).enviar(message, pdf);
        ordem.verify(solicitacaoEtiquetaGateway).marcarConcluida(SOLICITACAO_ID);
        Mockito.verify(solicitacaoEtiquetaGateway, Mockito.never()).marcarErro(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("Deve ignorar a mensagem reentregue de uma solicitacao ja concluida, sem gerar PDF nem enviar e-mail")
    void deveIgnorarSolicitacaoJaConcluida() {
        Mockito.when(solicitacaoEtiquetaGateway.iniciarProcessamento(SOLICITACAO_ID, 1)).thenReturn(false);

        ProcessarSolicitacaoEtiquetaUseCase.Resultado resultado = criarUseCase().execute(mensagemValida());

        Assertions.assertEquals(ProcessarSolicitacaoEtiquetaUseCase.Resultado.JA_CONCLUIDA, resultado);
        Mockito.verifyNoInteractions(gerarPdfEtiquetaGateway, enviarEtiquetaPorEmailGateway);
        Mockito.verify(solicitacaoEtiquetaGateway, Mockito.never()).marcarConcluida(Mockito.anyString());
        Mockito.verify(solicitacaoEtiquetaGateway, Mockito.never()).marcarErro(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("Deve registrar o erro, nao enviar e-mail e relancar a excecao quando a geracao do PDF falha")
    void deveRegistrarErroQuandoFalhaGeracaoPdf() {
        IllegalStateException falha = new IllegalStateException("Erro ao gerar etiqueta PDF.");
        Mockito.when(solicitacaoEtiquetaGateway.iniciarProcessamento(SOLICITACAO_ID, 1)).thenReturn(true);
        Mockito.when(gerarPdfEtiquetaGateway.gerar(Mockito.any(Etiqueta.class))).thenThrow(falha);

        IllegalStateException lancada = Assertions.assertThrows(
                IllegalStateException.class,
                () -> criarUseCase().execute(mensagemValida())
        );

        Assertions.assertSame(falha, lancada);
        Mockito.verifyNoInteractions(enviarEtiquetaPorEmailGateway);
        Mockito.verify(solicitacaoEtiquetaGateway)
                .marcarErro(SOLICITACAO_ID, "IllegalStateException: Erro ao gerar etiqueta PDF.");
        Mockito.verify(solicitacaoEtiquetaGateway, Mockito.never()).marcarConcluida(Mockito.anyString());
    }

    @Test
    @DisplayName("Deve registrar o erro, nao concluir e relancar a excecao quando o envio do e-mail falha")
    void deveRegistrarErroQuandoFalhaEnvioEmail() {
        SolicitacaoEtiquetaMessage message = mensagemValida();
        byte[] pdf = "pdf".getBytes();
        FalhaEnvioEmailEtiquetaException falha = new FalhaEnvioEmailEtiquetaException("SMTP indisponivel", null);
        Mockito.when(solicitacaoEtiquetaGateway.iniciarProcessamento(SOLICITACAO_ID, 1)).thenReturn(true);
        Mockito.when(gerarPdfEtiquetaGateway.gerar(Mockito.any(Etiqueta.class))).thenReturn(pdf);
        Mockito.doThrow(falha).when(enviarEtiquetaPorEmailGateway).enviar(message, pdf);

        Assertions.assertThrows(FalhaEnvioEmailEtiquetaException.class, () -> criarUseCase().execute(message));

        Mockito.verify(solicitacaoEtiquetaGateway)
                .marcarErro(SOLICITACAO_ID, "FalhaEnvioEmailEtiquetaException: SMTP indisponivel");
        Mockito.verify(solicitacaoEtiquetaGateway, Mockito.never()).marcarConcluida(Mockito.anyString());
    }

    @Test
    @DisplayName("Deve preservar a excecao original quando tambem falha ao registrar o erro")
    void devePreservarExcecaoOriginalQuandoRegistroDoErroFalha() {
        IllegalStateException falha = new IllegalStateException("Erro ao gerar etiqueta PDF.");
        IllegalStateException falhaBanco = new IllegalStateException("banco indisponivel");
        Mockito.when(solicitacaoEtiquetaGateway.iniciarProcessamento(SOLICITACAO_ID, 1)).thenReturn(true);
        Mockito.when(gerarPdfEtiquetaGateway.gerar(Mockito.any(Etiqueta.class))).thenThrow(falha);
        Mockito.doThrow(falhaBanco).when(solicitacaoEtiquetaGateway).marcarErro(Mockito.anyString(), Mockito.anyString());

        IllegalStateException lancada = Assertions.assertThrows(
                IllegalStateException.class,
                () -> criarUseCase().execute(mensagemValida())
        );

        Assertions.assertSame(falha, lancada);
        Assertions.assertSame(falhaBanco, lancada.getSuppressed()[0]);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mensagensInvalidas")
    @DisplayName("Deve rejeitar mensagens invalidas sem tocar no banco, no PDF ou no e-mail")
    void deveRejeitarMensagemInvalida(String descricao, SolicitacaoEtiquetaMessage message) {
        Assertions.assertThrows(SolicitacaoEtiquetaInvalidaException.class, () -> criarUseCase().execute(message));

        Mockito.verifyNoInteractions(solicitacaoEtiquetaGateway, gerarPdfEtiquetaGateway, enviarEtiquetaPorEmailGateway);
    }

    static Stream<Object[]> mensagensInvalidas() {
        return Stream.of(
                new Object[]{"mensagem nula", null},
                new Object[]{"solicitacaoId nulo",
                        new SolicitacaoEtiquetaMessage(null, 1, "ABC-123", "Bosch", "Filtro", "2026-09-03T23:00")},
                new Object[]{"solicitacaoId em branco",
                        new SolicitacaoEtiquetaMessage(" ", 1, "ABC-123", "Bosch", "Filtro", "2026-09-03T23:00")},
                new Object[]{"solicitacaoId que nao e UUID",
                        new SolicitacaoEtiquetaMessage("nao-e-um-uuid", 1, "ABC-123", "Bosch", "Filtro", "2026-09-03T23:00")},
                new Object[]{"itemId nulo",
                        new SolicitacaoEtiquetaMessage(SOLICITACAO_ID, null, "ABC-123", "Bosch", "Filtro", "2026-09-03T23:00")},
                new Object[]{"itemId zero",
                        new SolicitacaoEtiquetaMessage(SOLICITACAO_ID, 0, "ABC-123", "Bosch", "Filtro", "2026-09-03T23:00")},
                new Object[]{"codigoInterno nulo",
                        new SolicitacaoEtiquetaMessage(SOLICITACAO_ID, 1, null, "Bosch", "Filtro", "2026-09-03T23:00")},
                new Object[]{"codigoInterno em branco",
                        new SolicitacaoEtiquetaMessage(SOLICITACAO_ID, 1, "  ", "Bosch", "Filtro", "2026-09-03T23:00")}
        );
    }
}
