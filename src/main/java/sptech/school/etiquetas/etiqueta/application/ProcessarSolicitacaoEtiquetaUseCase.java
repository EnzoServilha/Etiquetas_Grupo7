package sptech.school.etiquetas.etiqueta.application;

import sptech.school.etiquetas.etiqueta.domain.EnviarEtiquetaPorEmailGateway;
import sptech.school.etiquetas.etiqueta.domain.Etiqueta;
import sptech.school.etiquetas.etiqueta.domain.GerarPdfEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaInvalidaException;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaMessage;

import java.util.UUID;

public class ProcessarSolicitacaoEtiquetaUseCase {

    public enum Resultado {
        PROCESSADA,
        JA_CONCLUIDA
    }

    private final SolicitacaoEtiquetaGateway solicitacaoEtiquetaGateway;
    private final GerarPdfEtiquetaGateway gerarPdfEtiquetaGateway;
    private final EnviarEtiquetaPorEmailGateway enviarEtiquetaPorEmailGateway;

    public ProcessarSolicitacaoEtiquetaUseCase(
            SolicitacaoEtiquetaGateway solicitacaoEtiquetaGateway,
            GerarPdfEtiquetaGateway gerarPdfEtiquetaGateway,
            EnviarEtiquetaPorEmailGateway enviarEtiquetaPorEmailGateway
    ) {
        this.solicitacaoEtiquetaGateway = solicitacaoEtiquetaGateway;
        this.gerarPdfEtiquetaGateway = gerarPdfEtiquetaGateway;
        this.enviarEtiquetaPorEmailGateway = enviarEtiquetaPorEmailGateway;
    }

    public Resultado execute(SolicitacaoEtiquetaMessage message) {
        validar(message);

        String solicitacaoId = message.solicitacaoId();
        if (!solicitacaoEtiquetaGateway.iniciarProcessamento(solicitacaoId, message.itemId())) {
            return Resultado.JA_CONCLUIDA;
        }

        try {
            Etiqueta etiqueta = new Etiqueta(
                    message.codigoInterno(),
                    message.marca(),
                    message.descricao(),
                    message.dataCadastro()
            );
            byte[] pdf = gerarPdfEtiquetaGateway.gerar(etiqueta);
            enviarEtiquetaPorEmailGateway.enviar(message, pdf);
            solicitacaoEtiquetaGateway.marcarConcluida(solicitacaoId);
            return Resultado.PROCESSADA;
        } catch (RuntimeException ex) {
            registrarErro(solicitacaoId, ex);
            throw ex;
        }
    }

    private void validar(SolicitacaoEtiquetaMessage message) {
        if (message == null) {
            throw new SolicitacaoEtiquetaInvalidaException("Mensagem de solicitacao de etiqueta vazia.");
        }
        if (!uuidValido(message.solicitacaoId())) {
            throw new SolicitacaoEtiquetaInvalidaException("solicitacaoId ausente ou invalido (esperado um UUID).");
        }
        if (message.itemId() == null || message.itemId() <= 0) {
            throw new SolicitacaoEtiquetaInvalidaException("itemId ausente ou invalido.");
        }
        if (message.codigoInterno() == null || message.codigoInterno().isBlank()) {
            throw new SolicitacaoEtiquetaInvalidaException("codigoInterno e obrigatorio para gerar a etiqueta.");
        }
    }

    private boolean uuidValido(String valor) {
        if (valor == null || valor.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(valor);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void registrarErro(String solicitacaoId, RuntimeException falha) {
        try {
            solicitacaoEtiquetaGateway.marcarErro(solicitacaoId, mensagemErro(falha));
        } catch (RuntimeException ex) {
            falha.addSuppressed(ex);
        }
    }

    private String mensagemErro(RuntimeException ex) {
        String detalhe = ex.getMessage() == null ? "Falha ao processar etiqueta." : ex.getMessage();
        return ex.getClass().getSimpleName() + ": " + detalhe;
    }
}
