package sptech.school.etiquetas.etiqueta.domain;

/**
 * Mensagem recebida da fila com dados ausentes ou inconsistentes. Reprocessar não adianta,
 * por isso ela não passa pelas tentativas de retry e vai direto para a DLQ.
 */
public class SolicitacaoEtiquetaInvalidaException extends RuntimeException {

    public SolicitacaoEtiquetaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
