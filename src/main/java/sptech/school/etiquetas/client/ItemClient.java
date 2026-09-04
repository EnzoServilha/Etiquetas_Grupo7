package sptech.school.etiquetas.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import sptech.school.etiquetas.dto.ItemEtiquetaResponseDto;

@Component
public class ItemClient {

    private final RestClient backendRestClient;

    public ItemClient(RestClient backendRestClient) {
        this.backendRestClient = backendRestClient;
    }

    public ItemEtiquetaResponseDto buscarItemPorId(Integer itemId, String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Header Authorization é obrigatório.");
        }

        try {
            ItemEtiquetaResponseDto item = backendRestClient.get()
                    .uri("/itens/{id}", itemId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(ItemEtiquetaResponseDto.class);

            if (item == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta vazia ao consultar item no backend.");
            }

            return item;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado no backend principal.");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido ou sem permissão para consultar o item.");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao consultar item no backend principal.", ex);
        }
    }
}
