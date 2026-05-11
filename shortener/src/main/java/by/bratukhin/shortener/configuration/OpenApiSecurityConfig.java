package by.bratukhin.shortener.configuration;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;

///
/// Open api security configuration.
///
@Configuration
class OpenApiSecurityConfig {

    @Bean
    GroupedOpenApi apiWithJwtAuth() {
        return GroupedOpenApi.builder()
            .group("default")
            .pathsToMatch("/**")
            .addOpenApiCustomizer(OpenApiSecurityConfig::addSecuritySchemes)
            .build();
    }

    private static void addSecuritySchemes(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
        }

        components.addSecuritySchemes("bearerAuth", new SecurityScheme()
            .type(SecurityScheme.Type.OAUTH2)
            .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                .authorizationUrl("http://localhost:8180/realms/shortener/protocol/openid-connect/auth")
                .tokenUrl("http://localhost:8180/realms/shortener/protocol/openid-connect/token")))
        );

        openApi.setComponents(components);
    }
}
