package sptech.school.etiquetas.etiqueta.infrastructure.persistence;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiqueta;
import sptech.school.etiquetas.etiqueta.domain.StatusSolicitacaoEtiqueta;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(SolicitacaoEtiquetaJpaAdapter.class)
class SolicitacaoEtiquetaJpaAdapterTest {

    private static final String ID = "6f1c2d0e-3b5a-4c7d-9e8f-0a1b2c3d4e5f";

    @Autowired
    private SolicitacaoEtiquetaJpaAdapter adapter;

    @Test
    @DisplayName("A primeira entrega deve registrar a solicitacao como PROCESSANDO e autorizar o processamento")
    void primeiraEntregaDeveIniciarProcessamento() {
        boolean deveProcessar = adapter.iniciarProcessamento(ID, 1);

        Assertions.assertTrue(deveProcessar);
        SolicitacaoEtiqueta solicitacao = adapter.buscarPorId(ID).orElseThrow();
        Assertions.assertEquals(StatusSolicitacaoEtiqueta.PROCESSANDO, solicitacao.status());
        Assertions.assertEquals(1, solicitacao.itemId());
        Assertions.assertEquals(1, solicitacao.tentativas());
    }

    @Test
    @DisplayName("Reentrega de solicitacao CONCLUIDA nao deve ser processada novamente")
    void reentregaDeSolicitacaoConcluidaDeveSerIgnorada() {
        adapter.iniciarProcessamento(ID, 1);
        adapter.marcarConcluida(ID);

        boolean deveProcessar = adapter.iniciarProcessamento(ID, 1);

        Assertions.assertFalse(deveProcessar);
        SolicitacaoEtiqueta solicitacao = adapter.buscarPorId(ID).orElseThrow();
        Assertions.assertEquals(StatusSolicitacaoEtiqueta.CONCLUIDA, solicitacao.status());
        Assertions.assertEquals(1, solicitacao.tentativas());
        Assertions.assertNotNull(solicitacao.finalizadaEm());
    }

    @Test
    @DisplayName("Solicitacao com ERRO deve poder ser reprocessada (retry ou reentrega) e contar a tentativa")
    void solicitacaoComErroDeveSerReprocessada() {
        adapter.iniciarProcessamento(ID, 1);
        adapter.marcarErro(ID, "FalhaEnvioEmailEtiquetaException: SMTP indisponivel");
        SolicitacaoEtiqueta comErro = adapter.buscarPorId(ID).orElseThrow();
        Assertions.assertEquals(StatusSolicitacaoEtiqueta.ERRO, comErro.status());
        Assertions.assertEquals("FalhaEnvioEmailEtiquetaException: SMTP indisponivel", comErro.mensagemErro());

        boolean deveProcessar = adapter.iniciarProcessamento(ID, 1);

        Assertions.assertTrue(deveProcessar);
        SolicitacaoEtiqueta reprocessando = adapter.buscarPorId(ID).orElseThrow();
        Assertions.assertEquals(StatusSolicitacaoEtiqueta.PROCESSANDO, reprocessando.status());
        Assertions.assertEquals(2, reprocessando.tentativas());
        Assertions.assertNull(reprocessando.mensagemErro());
    }

    @Test
    @DisplayName("Solicitacao interrompida em PROCESSANDO (consumer caiu) deve ser reprocessada na reentrega")
    void solicitacaoInterrompidaDeveSerReprocessada() {
        adapter.iniciarProcessamento(ID, 1);

        boolean deveProcessar = adapter.iniciarProcessamento(ID, 1);

        Assertions.assertTrue(deveProcessar);
        Assertions.assertEquals(2, adapter.buscarPorId(ID).orElseThrow().tentativas());
    }

    @Test
    @DisplayName("Deve limitar o tamanho da mensagem de erro gravada")
    void deveLimitarMensagemDeErro() {
        adapter.iniciarProcessamento(ID, 1);

        adapter.marcarErro(ID, "x".repeat(5000));

        Assertions.assertEquals(1000, adapter.buscarPorId(ID).orElseThrow().mensagemErro().length());
    }
}
