package sptech.school.etiquetas.etiqueta.infrastructure.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sptech.school.etiquetas.etiqueta.application.GerarEtiquetaPdfUseCase;

@RestController
@RequestMapping("/etiquetas")
public class EtiquetaController {

    private final GerarEtiquetaPdfUseCase gerarEtiquetaPdfUseCase;

    public EtiquetaController(GerarEtiquetaPdfUseCase gerarEtiquetaPdfUseCase) {
        this.gerarEtiquetaPdfUseCase = gerarEtiquetaPdfUseCase;
    }

    @GetMapping("/{itemId}/pdf")
    public ResponseEntity<byte[]> gerarEtiqueta(
            @PathVariable Integer itemId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        GerarEtiquetaPdfUseCase.GerarEtiquetaPdfCommand command =
                new GerarEtiquetaPdfUseCase.GerarEtiquetaPdfCommand(itemId, authorizationHeader);
        byte[] pdf = gerarEtiquetaPdfUseCase.execute(command);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=etiqueta-item-" + itemId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
