package com.nanitabeta.backend.config;

import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Swagger UI（OpenAPI）の設定です。
 * <p>
 * すべての API に JWT（Bearer トークン）が必要であることを API の説明に加え、Swagger UI の Authorize ボタンから
 * JWT を入力して API を試せるようにします。API の認証そのものは SecurityConfig が行います。
 */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer",
    bearerFormat = "JWT", description = "Supabase Auth でログインして取得した access_token")
@OpenAPIDefinition(security = @SecurityRequirement(name = "bearerAuth"))
public class OpenApiConfig {
}
