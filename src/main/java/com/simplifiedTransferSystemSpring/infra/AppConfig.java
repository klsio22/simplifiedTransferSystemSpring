package com.simplifiedTransferSystemSpring.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // sensible defaults to avoid indefinitely blocked threads
        factory.setConnectTimeout(2000); // 2s
        factory.setReadTimeout(5000); // 5s
        return new RestTemplate(factory);
    }
} 
