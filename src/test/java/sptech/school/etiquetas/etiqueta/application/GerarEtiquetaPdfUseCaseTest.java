package sptech.school.etiquetas.etiqueta.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import sptech.school.etiquetas.etiqueta.domain.BuscarEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.Etiqueta;
import sptech.school.etiquetas.etiqueta.domain.GerarPdfEtiquetaGateway;

@ExtendWith(MockitoExtension.class)
class GerarEtiquetaPdfUseCaseTest {

    @Mock
    private BuscarEtiquetaGateway buscarEtiquetaGateway;

    @Mock
    private GerarPdfEtiquetaGateway gerarPdfEtiquetaGateway;

    @Test
    @DisplayName("Deve buscar a etiqueta e gerar o PDF")
    void deveGerarPdf() {
        GerarEtiquetaPdfUseCase useCase = new GerarEtiquetaPdfUseCase(buscarEtiquetaGateway, gerarPdfEtiquetaGateway);
        Etiqueta etiqueta = new Etiqueta("ABC-123", "Bosch", "Filtro de óleo", "2026-09-03T23:00");
        byte[] pdfEsperado = "pdf".getBytes();
        GerarEtiquetaPdfUseCase.GerarEtiquetaPdfCommand command =
                new GerarEtiquetaPdfUseCase.GerarEtiquetaPdfCommand(1, "Bearer token");

        Mockito.when(buscarEtiquetaGateway.buscarPorItemId(1, "Bearer token")).thenReturn(etiqueta);
        Mockito.when(gerarPdfEtiquetaGateway.gerar(etiqueta)).thenReturn(pdfEsperado);

        byte[] resultado = useCase.execute(command);

        Assertions.assertArrayEquals(pdfEsperado, resultado);
        Mockito.verify(buscarEtiquetaGateway).buscarPorItemId(1, "Bearer token");
        Mockito.verify(gerarPdfEtiquetaGateway).gerar(etiqueta);
    }
}
