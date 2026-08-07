package com.cryptoshield.order_service.config;

import io.netty.channel.ChannelOption;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10))
                .followRedirect(false);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
    @Bean
    public WebClient walletServiceWebClient(WebClient.Builder builder) {
        ConnectionProvider provider = ConnectionProvider.builder("wallet-pool")
                .maxConnections(100)
                .pendingAcquireMaxCount(500)
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(30));

        return builder
                .baseUrl("http://wallet-service")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}