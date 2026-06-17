package com.overdrive.config

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Overdrive API")
                .description("Distribution Opportunity Engine — REST API")
                .version("v1")
        )
        .externalDocs(
            ExternalDocumentation()
                .description("GitHub repository")
                .url("https://github.com/kevinthelago/overdrive-api")
        )
}
