package sptech.school.etiquetas.etiqueta.infrastructure.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do e-mail que recebe as etiquetas. Sem valores padrão de propósito: o destinatário
 * precisa ser informado (variável de ambiente ETIQUETA_EMAIL_DESTINATARIO) e a aplicação não sobe sem ele,
 * evitando que mensagens da fila sejam consumidas e descartadas por falta de configuração.
 *
 * @param destinatario e-mail do Marcos
 * @param remetente    opcional; quando vazio usa o usuário SMTP (spring.mail.username)
 */
@Validated
@ConfigurationProperties(prefix = "etiqueta.email")
public record EtiquetaEmailProperties(
        @NotBlank(message = "Defina a variavel de ambiente ETIQUETA_EMAIL_DESTINATARIO com o e-mail que recebera as etiquetas.")
        @Email
        String destinatario,
        String remetente
) {
}
