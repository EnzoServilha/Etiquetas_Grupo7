package sptech.school.etiquetas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient backendRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${backend.api.base-url}") String backendBaseUrl
    ) {
        return restClientBuilder.baseUrl(backendBaseUrl).build();
    }
}
