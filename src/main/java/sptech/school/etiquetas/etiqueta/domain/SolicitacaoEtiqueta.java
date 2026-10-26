package sptech.school.etiquetas.etiqueta.domain;

import java.time.LocalDateTime;

public record SolicitacaoEtiqueta(
        String id,
        Integer itemId,
        StatusSolicitacaoEtiqueta status,
        String mensagemErro,
        int tentativas,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm,
        LocalDateTime finalizadaEm
) {
}
