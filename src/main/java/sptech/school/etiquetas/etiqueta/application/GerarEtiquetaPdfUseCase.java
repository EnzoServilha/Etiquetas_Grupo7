package sptech.school.etiquetas.etiqueta.application;

import sptech.school.etiquetas.etiqueta.domain.BuscarEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.Etiqueta;
import sptech.school.etiquetas.etiqueta.domain.GerarPdfEtiquetaGateway;

public class GerarEtiquetaPdfUseCase {

    private final BuscarEtiquetaGateway buscarEtiquetaGateway;
    private final GerarPdfEtiquetaGateway gerarPdfEtiquetaGateway;

    public GerarEtiquetaPdfUseCase(
            BuscarEtiquetaGateway buscarEtiquetaGateway,
            GerarPdfEtiquetaGateway gerarPdfEtiquetaGateway
    ) {
        this.buscarEtiquetaGateway = buscarEtiquetaGateway;
        this.gerarPdfEtiquetaGateway = gerarPdfEtiquetaGateway;
    }

    public byte[] execute(GerarEtiquetaPdfCommand command) {
        Etiqueta etiqueta = buscarEtiquetaGateway.buscarPorItemId(command.itemId(), command.tokenAcesso());
        return gerarPdfEtiquetaGateway.gerar(etiqueta);
    }

    public record GerarEtiquetaPdfCommand(Integer itemId, String tokenAcesso) {
    }
}
