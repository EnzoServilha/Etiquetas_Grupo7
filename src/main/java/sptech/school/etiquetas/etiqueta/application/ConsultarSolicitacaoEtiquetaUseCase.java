package sptech.school.etiquetas.etiqueta.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiqueta;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaGateway;

public class ConsultarSolicitacaoEtiquetaUseCase {

    private final SolicitacaoEtiquetaGateway solicitacaoEtiquetaGateway;

    public ConsultarSolicitacaoEtiquetaUseCase(SolicitacaoEtiquetaGateway solicitacaoEtiquetaGateway) {
        this.solicitacaoEtiquetaGateway = solicitacaoEtiquetaGateway;
    }

    public SolicitacaoEtiqueta execute(String id) {
        return solicitacaoEtiquetaGateway.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitacao de etiqueta nao encontrada."));
    }
}
