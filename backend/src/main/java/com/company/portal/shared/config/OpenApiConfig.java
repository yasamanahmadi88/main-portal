package com.company.portal.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI portalOpenApi(PortalProperties properties) {
        Server server = new Server()
                .url(properties.getPublicBaseUrl())
                .description("Portal API");

        SecurityScheme sessionCookie = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(properties.getSession().getCookie().effectiveName())
                .description("Server-side session cookie issued after successful authentication.");

        SecurityScheme csrfHeader = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-XSRF-TOKEN")
                .description("CSRF token synchronized with the XSRF-TOKEN cookie.");

        return new OpenAPI()
                .info(new Info()
                        .title("Portal API")
                        .version("v1")
                        .description("Enterprise portal HTTP API")
                        .contact(new Contact().name("Portal Platform Team"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(server))
                .components(new Components()
                        .addSecuritySchemes("sessionCookie", sessionCookie)
                        .addSecuritySchemes("csrfHeader", csrfHeader))
                .addSecurityItem(new SecurityRequirement()
                        .addList("sessionCookie")
                        .addList("csrfHeader"));
    }
}
