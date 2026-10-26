package sptech.school.etiquetas.etiqueta.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sptech.school.etiquetas.etiqueta.application.ConsultarSolicitacaoEtiquetaUseCase;
import sptech.school.etiquetas.etiqueta.application.GerarEtiquetaPdfUseCase;
import sptech.school.etiquetas.etiqueta.application.ProcessarSolicitacaoEtiquetaUseCase;
import sptech.school.etiquetas.etiqueta.domain.BuscarEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.EnviarEtiquetaPorEmailGateway;
import sptech.school.etiquetas.etiqueta.domain.GerarPdfEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaGateway;

@Configuration
@EnableConfigurationProperties(EtiquetaEmailProperties.class)
public class EtiquetaConfig {

    @Bean
    public GerarEtiquetaPdfUseCase gerarEtiquetaPdfUseCase(
            BuscarEtiquetaGateway buscarEtiquetaGateway,
            GerarPdfEtiquetaGateway gerarPdfEtiquetaGateway
    ) {
        return new GerarEtiquetaPdfUseCase(buscarEtiquetaGateway, gerarPdfEtiquetaGateway);
    }

    @Bean
    public ProcessarSolicitacaoEtiquetaUseCase processarSolicitacaoEtiquetaUseCase(
            SolicitacaoEtiquetaGateway solicitacaoEtiquetaGateway,
            GerarPdfEtiquetaGateway gerarPdfEtiquetaGateway,
            EnviarEtiquetaPorEmailGateway enviarEtiquetaPorEmailGateway
    ) {
        return new ProcessarSolicitacaoEtiquetaUseCase(
                solicitacaoEtiquetaGateway,
                gerarPdfEtiquetaGateway,
                enviarEtiquetaPorEmailGateway
        );
    }

    @Bean
    public ConsultarSolicitacaoEtiquetaUseCase consultarSolicitacaoEtiquetaUseCase(
            SolicitacaoEtiquetaGateway solicitacaoEtiquetaGateway
    ) {
        return new ConsultarSolicitacaoEtiquetaUseCase(solicitacaoEtiquetaGateway);
    }
}
