package com.codeexecution.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private final Judge0Properties judge0Properties;

    public RestTemplateConfig(Judge0Properties judge0Properties) {
        this.judge0Properties = judge0Properties;
    }

    @Bean
    @Qualifier("judge0RestTemplate")
    public RestTemplate judge0RestTemplate() {
        // Configure connection pooling
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100); // Max total connections
        connectionManager.setDefaultMaxPerRoute(20); // Max connections per route

        // Configure timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(judge0Properties.getConnectionTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(judge0Properties.getReadTimeout()))
                .build();

        // Build HTTP client with pooling and timeouts
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // Use HttpComponentsClientHttpRequestFactory for better performance
        HttpComponentsClientHttpRequestFactory requestFactory = 
                new HttpComponentsClientHttpRequestFactory(httpClient);
        
        // Note: In HttpClient5, timeouts are set in the RequestConfig,
        // which we've already configured above
        
        return new RestTemplateBuilder()
                .requestFactory(() -> requestFactory)
                .build();
    }
}
