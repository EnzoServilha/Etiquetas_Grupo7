package sptech.school.etiquetas.etiqueta.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sptech.school.etiquetas.etiqueta.application.GerarEtiquetaPdfUseCase;
import sptech.school.etiquetas.etiqueta.domain.BuscarEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.GerarPdfEtiquetaGateway;

@Configuration
public class EtiquetaConfig {

    @Bean
    public GerarEtiquetaPdfUseCase gerarEtiquetaPdfUseCase(
            BuscarEtiquetaGateway buscarEtiquetaGateway,
            GerarPdfEtiquetaGateway gerarPdfEtiquetaGateway
    ) {
        return new GerarEtiquetaPdfUseCase(buscarEtiquetaGateway, gerarPdfEtiquetaGateway);
    }
}
