package com.vehiclewallpaper.backend.web;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import com.vehiclewallpaper.backend.user.UserAuthenticationService;
import com.vehiclewallpaper.backend.user.UserUnauthorizedException;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class DownloadController {

    private final Path catalogRoot;
    private final UserAuthenticationService userAuthenticationService;

    public DownloadController(com.vehiclewallpaper.backend.config.CatalogProperties catalogProperties,
                              UserAuthenticationService userAuthenticationService) {
        this.catalogRoot = Paths.get(catalogProperties.getRootPath()).toAbsolutePath().normalize();
        this.userAuthenticationService = userAuthenticationService;
    }

    @GetMapping("/download/{brand}/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> download(@PathVariable String brand,
                                             @PathVariable String filename,
                                             HttpServletRequest request) {
        if (userAuthenticationService.authenticateOptional(request) == null) {
            throw new UserUnauthorizedException("下载功能需要登录。");
        }

        Path filePath = catalogRoot.resolve(brand).resolve(filename).normalize();

        // 防路径穿越
        if (!filePath.startsWith(catalogRoot)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        // 根据扩展名设置 Content-Type
        String lowerName = filename.toLowerCase();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (lowerName.endsWith(".webp")) {
            mediaType = MediaType.parseMediaType("image/webp");
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (lowerName.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        }

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename).build().toString())
            .body(resource);
    }
}
