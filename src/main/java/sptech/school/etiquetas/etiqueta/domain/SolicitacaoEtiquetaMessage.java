package sptech.school.etiquetas.etiqueta.domain;

/**
 * Contrato da mensagem publicada pelo BackEnd_Grupo7 (producer) na fila de etiquetas.
 * O backend mantém a sua própria cópia deste record: o que liga os dois serviços é a exchange,
 * a routing key e o formato JSON abaixo.
 *
 * @param solicitacaoId UUID gerado pelo producer; é a chave de idempotência do consumer
 * @param dataCadastro  data/hora do cadastro do item em ISO-8601 (ex.: 2025-09-03T23:00:15)
 */
public record SolicitacaoEtiquetaMessage(
        String solicitacaoId,
        Integer itemId,
        String codigoInterno,
        String marca,
        String descricao,
        String dataCadastro
) {
}
