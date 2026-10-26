package sptech.school.etiquetas.etiqueta.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiqueta;
import sptech.school.etiquetas.etiqueta.domain.StatusSolicitacaoEtiqueta;

import java.time.LocalDateTime;

/**
 * Registro de controle de cada solicitação recebida da fila. O id é o {@code solicitacaoId} gerado
 * pelo producer, o que permite identificar reentregas da mesma mensagem.
 */
@Entity
@Table(name = "solicitacao_etiqueta")
public class SolicitacaoEtiquetaEntity {

    private static final int TAMANHO_MAXIMO_MENSAGEM_ERRO = 1000;

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Integer itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacaoEtiqueta status;

    @Column(length = TAMANHO_MAXIMO_MENSAGEM_ERRO)
    private String mensagemErro;

    @Column(nullable = false)
    private int tentativas;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    @Column(nullable = false)
    private LocalDateTime atualizadaEm;

    private LocalDateTime finalizadaEm;

    protected SolicitacaoEtiquetaEntity() {
    }

    private SolicitacaoEtiquetaEntity(String id, Integer itemId) {
        LocalDateTime agora = LocalDateTime.now();
        this.id = id;
        this.itemId = itemId;
        this.status = StatusSolicitacaoEtiqueta.PROCESSANDO;
        this.tentativas = 1;
        this.criadaEm = agora;
        this.atualizadaEm = agora;
    }

    public static SolicitacaoEtiquetaEntity iniciar(String id, Integer itemId) {
        return new SolicitacaoEtiquetaEntity(id, itemId);
    }

    public void marcarProcessando() {
        this.status = StatusSolicitacaoEtiqueta.PROCESSANDO;
        this.tentativas++;
        this.mensagemErro = null;
        this.finalizadaEm = null;
        this.atualizadaEm = LocalDateTime.now();
    }

    public void marcarConcluida() {
        LocalDateTime agora = LocalDateTime.now();
        this.status = StatusSolicitacaoEtiqueta.CONCLUIDA;
        this.mensagemErro = null;
        this.atualizadaEm = agora;
        this.finalizadaEm = agora;
    }

    public void marcarErro(String mensagemErro) {
        LocalDateTime agora = LocalDateTime.now();
        this.status = StatusSolicitacaoEtiqueta.ERRO;
        this.mensagemErro = limitarMensagemErro(mensagemErro);
        this.atualizadaEm = agora;
        this.finalizadaEm = agora;
    }

    public StatusSolicitacaoEtiqueta getStatus() {
        return status;
    }

    public SolicitacaoEtiqueta toDomain() {
        return new SolicitacaoEtiqueta(
                id,
                itemId,
                status,
                mensagemErro,
                tentativas,
                criadaEm,
                atualizadaEm,
                finalizadaEm
        );
    }

    private String limitarMensagemErro(String mensagemErro) {
        String mensagem = mensagemErro == null || mensagemErro.isBlank()
                ? "Falha ao processar solicitacao de etiqueta."
                : mensagemErro;
        if (mensagem.length() <= TAMANHO_MAXIMO_MENSAGEM_ERRO) {
            return mensagem;
        }
        return mensagem.substring(0, TAMANHO_MAXIMO_MENSAGEM_ERRO);
    }
}
