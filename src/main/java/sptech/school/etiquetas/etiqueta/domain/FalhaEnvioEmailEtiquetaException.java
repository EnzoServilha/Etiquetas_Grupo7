package sptech.school.etiquetas.etiqueta.domain;

public class FalhaEnvioEmailEtiquetaException extends RuntimeException {

    public FalhaEnvioEmailEtiquetaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
