/*
 * Declara beans tecnicos compartidos por los adaptadores de infraestructura.
 */
package ec.edu.unl.redhospitales.infraestructura.configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ConfiguracionInfraestructura {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(1000))
                .setReadTimeout(Duration.ofMillis(1500))
                .build();
    }

    @Bean
    public com.ecwid.consul.v1.ConsulClient consulClient(
            @org.springframework.beans.factory.annotation.Value("${spring.cloud.consul.host:localhost}") String consulHost,
            @org.springframework.beans.factory.annotation.Value("${spring.cloud.consul.port:8500}") int consulPort
    ) {
        int timeoutMillis = 1500;
        org.apache.http.client.config.RequestConfig requestConfig = org.apache.http.client.config.RequestConfig.custom()
                .setConnectTimeout(timeoutMillis)
                .setSocketTimeout(timeoutMillis)
                .setConnectionRequestTimeout(timeoutMillis)
                .build();

        org.apache.http.impl.client.CloseableHttpClient httpClient = org.apache.http.impl.client.HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();

        com.ecwid.consul.v1.ConsulRawClient rawClient = com.ecwid.consul.v1.ConsulRawClient.Builder.builder()
                .setHost(consulHost)
                .setPort(consulPort)
                .setHttpClient(httpClient)
                .build();

        return new com.ecwid.consul.v1.ConsulClient(rawClient);
    }
}
