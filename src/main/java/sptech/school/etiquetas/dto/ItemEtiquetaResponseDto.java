package sptech.school.etiquetas.dto;

import java.time.LocalDateTime;

public record ItemEtiquetaResponseDto(
        Integer id,
        String codigoInterno,
        String marca,
        String descricao,
        LocalDateTime dataCadastro
) {
}
