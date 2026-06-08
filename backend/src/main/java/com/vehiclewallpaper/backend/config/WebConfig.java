package com.vehiclewallpaper.backend.config;

import com.vehiclewallpaper.backend.admin.AdminApiKeyInterceptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
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

        registry.addMapping("/download/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "OPTIONS")
            .allowedHeaders("*");

        registry.addMapping("/download/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders(HttpHeaders.CONTENT_DISPOSITION, HttpHeaders.CONTENT_TYPE);
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
        CacheControl htmlCacheControl = CacheControl.noStore();
        CacheControl immutableAssetCacheControl = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic();
        CacheControl metadataCacheControl = CacheControl.noCache();

        registry.addResourceHandler("/*.html")
            .addResourceLocations(frontendLocation)
            .setCacheControl(htmlCacheControl);

        registry.addResourceHandler("/*.css", "/*.js")
            .addResourceLocations(frontendLocation)
            .setCacheControl(immutableAssetCacheControl);

        registry.addResourceHandler("/*.txt", "/*.xml")
            .addResourceLocations(frontendLocation)
            .setCacheControl(metadataCacheControl);

        registry.addResourceHandler("/cars/**")
            .addResourceLocations(catalogLocation)
            .setCacheControl(immutableAssetCacheControl);
    }

    private String toResourceLocation(String rootPath) {
        Path absolutePath = Paths.get(rootPath).toAbsolutePath().normalize();
        return absolutePath.toUri().toString();
    }
}
