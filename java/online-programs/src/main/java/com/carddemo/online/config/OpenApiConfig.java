package com.carddemo.online.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("CardDemo Online API")
                .version("1.0.0")
                .description("REST API for CardDemo credit card application (COBOL to Java migration)")
                .contact(new Contact()
                    .name("CardDemo")
                    .url("https://github.com/slakkojulearnings/mainframetojava")))
            .servers(Arrays.asList(
                new Server()
                    .url("http://localhost:8080/carddemo")
                    .description("Development Server")
            ));
    }
}
