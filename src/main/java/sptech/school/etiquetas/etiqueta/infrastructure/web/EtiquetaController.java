package sptech.school.etiquetas.etiqueta.infrastructure.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sptech.school.etiquetas.etiqueta.application.ConsultarSolicitacaoEtiquetaUseCase;
import sptech.school.etiquetas.etiqueta.application.GerarEtiquetaPdfUseCase;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiqueta;
import sptech.school.etiquetas.etiqueta.domain.StatusSolicitacaoEtiqueta;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/etiquetas")
public class EtiquetaController {

    private final GerarEtiquetaPdfUseCase gerarEtiquetaPdfUseCase;
    private final ConsultarSolicitacaoEtiquetaUseCase consultarSolicitacaoEtiquetaUseCase;

    public EtiquetaController(
            GerarEtiquetaPdfUseCase gerarEtiquetaPdfUseCase,
            ConsultarSolicitacaoEtiquetaUseCase consultarSolicitacaoEtiquetaUseCase
    ) {
        this.gerarEtiquetaPdfUseCase = gerarEtiquetaPdfUseCase;
        this.consultarSolicitacaoEtiquetaUseCase = consultarSolicitacaoEtiquetaUseCase;
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

    @GetMapping("/solicitacoes/{id}")
    public ResponseEntity<SolicitacaoEtiquetaResponse> consultarSolicitacao(@PathVariable String id) {
        SolicitacaoEtiqueta solicitacao = consultarSolicitacaoEtiquetaUseCase.execute(id);
        return ResponseEntity.ok(SolicitacaoEtiquetaResponse.from(solicitacao));
    }

    public record SolicitacaoEtiquetaResponse(
            String id,
            Integer itemId,
            StatusSolicitacaoEtiqueta status,
            String mensagemErro,
            int tentativas,
            LocalDateTime criadaEm,
            LocalDateTime atualizadaEm,
            LocalDateTime finalizadaEm
    ) {
        static SolicitacaoEtiquetaResponse from(SolicitacaoEtiqueta solicitacao) {
            return new SolicitacaoEtiquetaResponse(
                    solicitacao.id(),
                    solicitacao.itemId(),
                    solicitacao.status(),
                    solicitacao.mensagemErro(),
                    solicitacao.tentativas(),
                    solicitacao.criadaEm(),
                    solicitacao.atualizadaEm(),
                    solicitacao.finalizadaEm()
            );
        }
    }
}
