package sptech.school.etiquetas.etiqueta.infrastructure.adapter;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import sptech.school.etiquetas.etiqueta.domain.FalhaEnvioEmailEtiquetaException;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaMessage;
import sptech.school.etiquetas.etiqueta.infrastructure.config.EtiquetaEmailProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

class EnviarEtiquetaPorEmailJavaMailAdapterTest {

    private static final String SOLICITACAO_ID = "6f1c2d0e-3b5a-4c7d-9e8f-0a1b2c3d4e5f";
    private static final byte[] PDF = "%PDF-1.4 conteudo de teste".getBytes(StandardCharsets.US_ASCII);

    private CapturaMailSender mailSender;

    @BeforeEach
    void setUp() {
        mailSender = new CapturaMailSender();
    }

    private SolicitacaoEtiquetaMessage solicitacao() {
        return new SolicitacaoEtiquetaMessage(SOLICITACAO_ID, 7, "ABC-123", "Bosch", "Filtro de óleo", "2026-09-03T23:00");
    }

    @Test
    @DisplayName("Deve enviar o PDF anexado para o destinatario configurado")
    void deveEnviarPdfAnexado() throws Exception {
        EnviarEtiquetaPorEmailJavaMailAdapter adapter = new EnviarEtiquetaPorEmailJavaMailAdapter(
                mailSender,
                new EtiquetaEmailProperties("marcos@example.com", "etiquetas@example.com"),
                "usuario-smtp@example.com"
        );

        adapter.enviar(solicitacao(), PDF);

        MimeMessage enviado = reler(mailSender.enviado);
        Assertions.assertEquals("marcos@example.com", ((InternetAddress) enviado.getRecipients(Message.RecipientType.TO)[0]).getAddress());
        Assertions.assertEquals("etiquetas@example.com", ((InternetAddress) enviado.getFrom()[0]).getAddress());
        Assertions.assertEquals("Etiqueta do item ABC-123", enviado.getSubject());

        List<Part> partes = new ArrayList<>();
        coletarPartes(enviado, partes);

        Part anexo = partes.stream()
                .filter(parte -> uncheck(() -> Part.ATTACHMENT.equalsIgnoreCase(parte.getDisposition())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Nenhum anexo encontrado no e-mail"));
        Assertions.assertEquals("etiqueta-item-7.pdf", anexo.getFileName());
        Assertions.assertTrue(anexo.getContentType().toLowerCase().startsWith("application/pdf"));
        Assertions.assertArrayEquals(PDF, lerBytes(anexo.getInputStream()));

        String corpo = partes.stream()
                .filter(parte -> uncheck(() -> parte.isMimeType("text/plain") && parte.getDisposition() == null))
                .map(parte -> uncheck(() -> (String) parte.getContent()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Corpo de texto nao encontrado"));
        Assertions.assertTrue(corpo.contains("ABC-123"));
        Assertions.assertTrue(corpo.contains("Filtro de óleo"));
        Assertions.assertTrue(corpo.contains(SOLICITACAO_ID));
    }

    @Test
    @DisplayName("Deve usar o usuario SMTP como remetente quando nenhum remetente foi configurado")
    void deveUsarUsuarioSmtpComoRemetente() throws Exception {
        EnviarEtiquetaPorEmailJavaMailAdapter adapter = new EnviarEtiquetaPorEmailJavaMailAdapter(
                mailSender,
                new EtiquetaEmailProperties("marcos@example.com", ""),
                "usuario-smtp@example.com"
        );

        adapter.enviar(solicitacao(), PDF);

        Assertions.assertEquals("usuario-smtp@example.com",
                ((InternetAddress) reler(mailSender.enviado).getFrom()[0]).getAddress());
    }

    @Test
    @DisplayName("Deve remover quebras de linha do codigo interno no assunto")
    void deveRemoverQuebrasDeLinhaDoAssunto() throws Exception {
        EnviarEtiquetaPorEmailJavaMailAdapter adapter = new EnviarEtiquetaPorEmailJavaMailAdapter(
                mailSender, new EtiquetaEmailProperties("marcos@example.com", null), "");
        SolicitacaoEtiquetaMessage message =
                new SolicitacaoEtiquetaMessage(SOLICITACAO_ID, 7, "ABC\r\nBcc: alguem@example.com", "Bosch", null, null);

        adapter.enviar(message, PDF);

        MimeMessage enviado = reler(mailSender.enviado);
        Assertions.assertEquals("Etiqueta do item ABC Bcc: alguem@example.com", enviado.getSubject());
        Assertions.assertNull(enviado.getRecipients(Message.RecipientType.BCC));
    }

    @Test
    @DisplayName("Deve lancar FalhaEnvioEmailEtiquetaException quando o servidor SMTP falha")
    void deveLancarExcecaoQuandoSmtpFalha() {
        mailSender.falha = new MailSendException("SMTP indisponivel");
        EnviarEtiquetaPorEmailJavaMailAdapter adapter = new EnviarEtiquetaPorEmailJavaMailAdapter(
                mailSender, new EtiquetaEmailProperties("marcos@example.com", null), "usuario-smtp@example.com");

        FalhaEnvioEmailEtiquetaException ex = Assertions.assertThrows(
                FalhaEnvioEmailEtiquetaException.class,
                () -> adapter.enviar(solicitacao(), PDF)
        );

        Assertions.assertTrue(ex.getMessage().contains(SOLICITACAO_ID));
        Assertions.assertInstanceOf(MailSendException.class, ex.getCause());
    }

    private static final class CapturaMailSender extends JavaMailSenderImpl {

        MimeMessage enviado;
        RuntimeException falha;

        CapturaMailSender() {
            setSession(Session.getInstance(new Properties()));
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            if (falha != null) {
                throw falha;
            }
            this.enviado = mimeMessage;
        }
    }

    // Serializa e le de novo a mensagem para validar exatamente o que iria pela rede
    private MimeMessage reler(MimeMessage original) throws Exception {
        original.saveChanges();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        original.writeTo(saida);
        return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(saida.toByteArray()));
    }

    private void coletarPartes(Part parte, List<Part> destino) throws Exception {
        if (parte.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) parte.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart filha = multipart.getBodyPart(i);
                coletarPartes(filha, destino);
            }
        } else {
            destino.add(parte);
        }
    }

    private byte[] lerBytes(InputStream stream) throws Exception {
        try (stream) {
            return stream.readAllBytes();
        }
    }

    private interface Acao<T> {
        T executar() throws Exception;
    }

    private static <T> T uncheck(Acao<T> acao) {
        try {
            return acao.executar();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
