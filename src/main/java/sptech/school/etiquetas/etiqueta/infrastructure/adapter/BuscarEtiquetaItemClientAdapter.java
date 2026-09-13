package sptech.school.etiquetas.etiqueta.infrastructure.adapter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import sptech.school.etiquetas.etiqueta.domain.BuscarEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.Etiqueta;

import java.time.LocalDateTime;

@Component
public class BuscarEtiquetaItemClientAdapter implements BuscarEtiquetaGateway {

    private final RestClient backendRestClient;

    public BuscarEtiquetaItemClientAdapter(RestClient backendRestClient) {
        this.backendRestClient = backendRestClient;
    }

    @Override
    public Etiqueta buscarPorItemId(Integer itemId, String tokenAcesso) {
        if (tokenAcesso == null || tokenAcesso.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Header Authorization é obrigatório.");
        }

        try {
            ItemEtiquetaClientResponse response = backendRestClient.get()
                    .uri("/itens/{id}", itemId)
                    .header(HttpHeaders.AUTHORIZATION, tokenAcesso)
                    .retrieve()
                    .body(ItemEtiquetaClientResponse.class);

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta vazia ao consultar item no backend.");
            }

            return new Etiqueta(
                    response.codigoInterno(),
                    response.marca(),
                    response.descricao(),
                    response.dataCadastro() != null ? response.dataCadastro().toString() : ""
            );
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado no backend principal.");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido ou sem permissão para consultar o item.");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao consultar item no backend principal.", ex);
        }
    }

    record ItemEtiquetaClientResponse(
            Integer id,
            String codigoInterno,
            String marca,
            String descricao,
            LocalDateTime dataCadastro
    ) {
    }
}
