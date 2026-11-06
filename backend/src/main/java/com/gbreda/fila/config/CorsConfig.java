package com.gbreda.fila.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConfigurationProperties(prefix = "fila")
public class CorsConfig implements WebMvcConfigurer {

    private List<String> corsOriginPatterns = List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost",
            "capacitor://localhost",
            "ionic://localhost",
            "http://localhost");

    public List<String> getCorsOriginPatterns() {
        return corsOriginPatterns;
    }

    public void setCorsOriginPatterns(List<String> corsOriginPatterns) {
        this.corsOriginPatterns = corsOriginPatterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(corsOriginPatterns.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
