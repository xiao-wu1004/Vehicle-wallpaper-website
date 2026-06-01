package com.vehiclewallpaper.backend;

import com.vehiclewallpaper.backend.config.CatalogProperties;
import com.vehiclewallpaper.backend.config.FrontendProperties;
import com.vehiclewallpaper.backend.config.AdminSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({CatalogProperties.class, FrontendProperties.class, AdminSecurityProperties.class})
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
