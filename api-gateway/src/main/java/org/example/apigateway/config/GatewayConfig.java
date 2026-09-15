package org.example.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth", r -> r.path("/auth/**")
                        .uri("lb://authentication-service"))
                .route("inventory", r -> r.path("/inventory/**")
                        .uri("lb://inventory-service"))
                .route("rental", r -> r.path("/rental/**")
                        .uri("lb://rental-reservation-service"))
                .build();
    }
}
