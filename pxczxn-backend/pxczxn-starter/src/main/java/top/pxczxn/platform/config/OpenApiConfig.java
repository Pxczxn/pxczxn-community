package top.pxczxn.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.1 配置
 *
 * Phase 0: 仅配置基础设施，试点 Admin Community Users API
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("星语社区 API")
                .version("1.0.0")
                .description("星语社区后端 API - Phase 0 Pilot")
                .contact(new Contact()
                    .name("Pxczxn")
                    .url("https://github.com/Pxczxn/pxczxn-community")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("本地开发"),
                new Server().url("https://api.example.com").description("生产环境（待配置）")
            ));
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .displayName("管理端 API")
            .pathsToMatch("/admin-api/**")
            .build();
    }

    @Bean
    public GroupedOpenApi webApi() {
        return GroupedOpenApi.builder()
            .group("web")
            .displayName("用户端 API")
            .pathsToMatch("/api/v1/**")
            .build();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .displayName("公开 API")
            .pathsToMatch("/api/v1/public/**")
            .build();
    }
}
