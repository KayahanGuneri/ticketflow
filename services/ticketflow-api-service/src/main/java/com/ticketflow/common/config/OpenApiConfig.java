package com.ticketflow.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TicketFlow API",
                version = "v1",
                description = "Event-driven ticket reservation and order orchestration platform API"
        )
)
public class OpenApiConfig {
}