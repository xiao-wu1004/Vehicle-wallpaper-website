package com.vehiclewallpaper.backend.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FrontendProperties frontendProperties;
    private final CatalogProperties catalogProperties;

    public WebConfig(FrontendProperties frontendProperties, CatalogProperties catalogProperties) {
        this.frontendProperties = frontendProperties;
        this.catalogProperties = catalogProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST")
            .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String frontendLocation = toResourceLocation(frontendProperties.getRootPath());
        String catalogLocation = toResourceLocation(catalogProperties.getRootPath());

        registry.addResourceHandler("/*.html", "/*.css", "/*.js", "/*.txt", "/*.xml")
            .addResourceLocations(frontendLocation);

        registry.addResourceHandler("/cars/**")
            .addResourceLocations(catalogLocation);
    }

    private String toResourceLocation(String rootPath) {
        Path absolutePath = Paths.get(rootPath).toAbsolutePath().normalize();
        return absolutePath.toUri().toString();
    }
}
