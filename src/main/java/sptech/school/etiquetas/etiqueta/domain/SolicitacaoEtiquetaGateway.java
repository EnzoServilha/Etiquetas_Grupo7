package sptech.school.etiquetas.etiqueta.domain;

import java.util.Optional;

public interface SolicitacaoEtiquetaGateway {

    Optional<SolicitacaoEtiqueta> buscarPorId(String id);

    /**
     * Registra o início de uma tentativa de processamento.
     *
     * @return {@code false} se a solicitação já foi concluída (reentrega duplicada) e não deve ser processada de novo
     */
    boolean iniciarProcessamento(String id, Integer itemId);

    void marcarConcluida(String id);

    void marcarErro(String id, String mensagemErro);
}
