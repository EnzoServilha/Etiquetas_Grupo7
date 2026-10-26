package sptech.school.etiquetas.etiqueta.infrastructure.adapter;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import sptech.school.etiquetas.etiqueta.domain.EnviarEtiquetaPorEmailGateway;
import sptech.school.etiquetas.etiqueta.domain.FalhaEnvioEmailEtiquetaException;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaMessage;
import sptech.school.etiquetas.etiqueta.infrastructure.config.EtiquetaEmailProperties;

import java.nio.charset.StandardCharsets;

@Component
public class EnviarEtiquetaPorEmailJavaMailAdapter implements EnviarEtiquetaPorEmailGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnviarEtiquetaPorEmailJavaMailAdapter.class);

    private final JavaMailSender javaMailSender;
    private final EtiquetaEmailProperties emailProperties;
    private final String usuarioSmtp;

    public EnviarEtiquetaPorEmailJavaMailAdapter(
            JavaMailSender javaMailSender,
            EtiquetaEmailProperties emailProperties,
            @Value("${spring.mail.username:}") String usuarioSmtp
    ) {
        this.javaMailSender = javaMailSender;
        this.emailProperties = emailProperties;
        this.usuarioSmtp = usuarioSmtp;
    }

    @Override
    public void enviar(SolicitacaoEtiquetaMessage solicitacao, byte[] pdf) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            String remetente = remetente();
            if (!remetente.isBlank()) {
                helper.setFrom(remetente);
            }
            helper.setTo(emailProperties.destinatario());
            helper.setSubject("Etiqueta do item " + semQuebrasDeLinha(solicitacao.codigoInterno()));
            helper.setText(corpo(solicitacao), false);
            helper.addAttachment(
                    "etiqueta-item-" + solicitacao.itemId() + ".pdf",
                    new ByteArrayResource(pdf),
                    "application/pdf"
            );

            javaMailSender.send(mimeMessage);
            LOGGER.info("E-mail com a etiqueta enviado: solicitacaoId={}, itemId={}, pdfBytes={}.",
                    solicitacao.solicitacaoId(), solicitacao.itemId(), pdf.length);
        } catch (MessagingException | MailException ex) {
            throw new FalhaEnvioEmailEtiquetaException(
                    "Falha ao enviar o e-mail da etiqueta (solicitacaoId=" + solicitacao.solicitacaoId() + ").",
                    ex
            );
        }
    }

    private String remetente() {
        String remetente = emailProperties.remetente();
        if (remetente != null && !remetente.isBlank()) {
            return remetente;
        }
        return usuarioSmtp == null ? "" : usuarioSmtp;
    }

    private String corpo(SolicitacaoEtiquetaMessage solicitacao) {
        return """
                Olá, Marcos!

                Segue em anexo (PDF) a etiqueta do item solicitado no sistema.

                Código interno: %s
                Marca: %s
                Descrição: %s
                Data de cadastro: %s
                Solicitação: %s

                Esta é uma mensagem automática.
                """.formatted(
                semQuebrasDeLinha(solicitacao.codigoInterno()),
                valorOuTraco(solicitacao.marca()),
                valorOuTraco(solicitacao.descricao()),
                valorOuTraco(solicitacao.dataCadastro()),
                solicitacao.solicitacaoId()
        );
    }

    private String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private String semQuebrasDeLinha(String valor) {
        return valor == null ? "" : valor.replaceAll("[\\r\\n]+", " ").trim();
    }
}
