package com.vehiclewallpaper.backend.config;

import com.vehiclewallpaper.backend.admin.AdminApiKeyInterceptor;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FrontendProperties frontendProperties;
    private final CatalogProperties catalogProperties;
    private final AdminApiKeyInterceptor adminApiKeyInterceptor;

    public WebConfig(FrontendProperties frontendProperties,
                     CatalogProperties catalogProperties,
                     AdminApiKeyInterceptor adminApiKeyInterceptor) {
        this.frontendProperties = frontendProperties;
        this.catalogProperties = catalogProperties;
        this.adminApiKeyInterceptor = adminApiKeyInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*");

        registry.addMapping("/cars/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "OPTIONS")
            .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminApiKeyInterceptor)
            .addPathPatterns("/api/admin/**")
            .excludePathPatterns("/api/admin/auth/login", "/api/admin/auth/options");
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
