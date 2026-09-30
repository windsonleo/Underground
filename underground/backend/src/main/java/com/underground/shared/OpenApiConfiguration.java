package com.underground.shared;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.*;
import io.swagger.v3.oas.models.responses.*;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;

@Configuration
class OpenApiConfiguration {
    @Bean OpenAPI api() {
        var loginBody = new ObjectSchema()
            .addProperty("email", new StringSchema().format("email"))
            .addProperty("password", new StringSchema().format("password"))
            .addRequiredItem("email").addRequiredItem("password");
        return new OpenAPI().info(new Info().title("Underground API").version("0.1.0"))
            .components(new Components().addSecuritySchemes("session",
                new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")))
            .path("/api/auth/login", new PathItem().post(new Operation().summary("Authenticate using email and password")
                .description("Fetch /api/auth/csrf first and preserve cookies. Fetch a new CSRF token after login.")
                .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                    "application/x-www-form-urlencoded", new MediaType().schema(loginBody))))
                .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Authenticated; session cookie renewed"))
                    .addApiResponse("401", new ApiResponse().description("Invalid credentials")))))
            .path("/api/auth/logout", new PathItem().post(new Operation().summary("Invalidate session")
                .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Logged out")))));
    }

    @Bean OpenApiCustomizer accessContracts() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            if (!path.equals("/api/auth/csrf") && !path.equals("/api/auth/login") && !path.equals("/api/auth/register")) {
                operation.addSecurityItem(new SecurityRequirement().addList("session"));
            }
            if (method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PUT) {
                operation.addParametersItem(new HeaderParameter().name("X-CSRF-TOKEN").required(true)
                    .description("Token returned by GET /api/auth/csrf for the current session")
                    .schema(new StringSchema()));
                operation.getResponses().addApiResponse("403", new ApiResponse().description("Invalid CSRF token or forbidden access"));
            }
        }));
    }
}
