package sptech.school.etiquetas.etiqueta.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class EtiquetaEmailPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropriedadesConfig.class);

    @Test
    @DisplayName("Deve carregar o destinatario configurado")
    void deveCarregarDestinatario() {
        contextRunner
                .withPropertyValues("etiqueta.email.destinatario=marcos@example.com")
                .run(contexto -> {
                    assertThat(contexto).hasNotFailed();
                    assertThat(contexto.getBean(EtiquetaEmailProperties.class).destinatario()).isEqualTo("marcos@example.com");
                });
    }

    @Test
    @DisplayName("Nao deve subir sem o destinatario, para nao consumir e descartar mensagens da fila")
    void naoDeveSubirSemDestinatario() {
        contextRunner
                .withPropertyValues("etiqueta.email.destinatario=")
                .run(contexto -> {
                    assertThat(contexto).hasFailed();
                    assertThat(contexto.getStartupFailure()).hasStackTraceContaining("etiqueta.email.destinatario");
                });
    }

    @Test
    @DisplayName("Nao deve subir com um destinatario que nao e e-mail")
    void naoDeveSubirComEmailInvalido() {
        contextRunner
                .withPropertyValues("etiqueta.email.destinatario=isto-nao-e-email")
                .run(contexto -> assertThat(contexto).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(EtiquetaEmailProperties.class)
    static class PropriedadesConfig {
    }
}
