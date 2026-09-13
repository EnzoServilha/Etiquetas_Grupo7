package sptech.school.etiquetas.etiqueta.infrastructure.adapter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sptech.school.etiquetas.etiqueta.domain.Etiqueta;

import java.nio.charset.StandardCharsets;

class GerarPdfEtiquetaJasperAdapterTest {

    @Test
    @DisplayName("Deve gerar um PDF válido a partir do template")
    void deveGerarPdfValido() {
        GerarPdfEtiquetaJasperAdapter adapter = new GerarPdfEtiquetaJasperAdapter();
        Etiqueta etiqueta = new Etiqueta("ABC-123", "Bosch", "Filtro de óleo", "2026-09-03T23:00");

        byte[] resultado = adapter.gerar(etiqueta);

        Assertions.assertTrue(resultado.length > 4);
        Assertions.assertEquals("%PDF", new String(resultado, 0, 4, StandardCharsets.US_ASCII));
    }
}
