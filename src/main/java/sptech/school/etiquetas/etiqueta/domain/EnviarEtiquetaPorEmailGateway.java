package sptech.school.etiquetas.etiqueta.domain;

public interface EnviarEtiquetaPorEmailGateway {

    void enviar(SolicitacaoEtiquetaMessage solicitacao, byte[] pdf);
}
