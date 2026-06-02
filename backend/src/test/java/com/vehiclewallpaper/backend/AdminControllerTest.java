package com.vehiclewallpaper.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclewallpaper.backend.catalog.BrandEntity;
import com.vehiclewallpaper.backend.catalog.BrandRepository;
import com.vehiclewallpaper.backend.catalog.WallpaperEntity;
import com.vehiclewallpaper.backend.catalog.WallpaperRepository;
import com.vehiclewallpaper.backend.feedback.FeedbackMessage;
import com.vehiclewallpaper.backend.feedback.FeedbackRepository;
import com.vehiclewallpaper.backend.feedback.FeedbackStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    private static final String ADMIN_HEADER = "X-Admin-API-Key";
    private static final String ADMIN_KEY = "test-admin-key";
    private static final Path TEST_CATALOG_ROOT = createTestCatalogRoot();

    @DynamicPropertySource
    static void registerCatalogRoot(DynamicPropertyRegistry registry) {
        registry.add("app.catalog.root-path", () -> TEST_CATALOG_ROOT.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private WallpaperRepository wallpaperRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndDeleteBrand() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/admin/brands")
                .header(ADMIN_HEADER, ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"zeekr\",\"displayName\":\"Zeekr\",\"folderName\":\"Zeekr\",\"sortOrder\":25}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("zeekr"))
            .andExpect(jsonPath("$.displayName").value("Zeekr"))
            .andExpect(jsonPath("$.folderName").value("Zeekr"))
            .andExpect(jsonPath("$.wallpaperCount").value(0))
            .andReturn();

        JsonNode createdBrand = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long brandId = createdBrand.get("id").asLong();

        org.junit.jupiter.api.Assertions.assertTrue(Files.isDirectory(TEST_CATALOG_ROOT.resolve("Zeekr")));
        org.junit.jupiter.api.Assertions.assertTrue(Files.isDirectory(TEST_CATALOG_ROOT.resolve("_thumb").resolve("Zeekr")));

        mockMvc.perform(delete("/api/admin/brands/{brandId}", brandId)
                .header(ADMIN_HEADER, ADMIN_KEY))
            .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(TEST_CATALOG_ROOT.resolve("Zeekr")));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(TEST_CATALOG_ROOT.resolve("_thumb").resolve("Zeekr")));
    }

    @Test
    void shouldExposeDashboardMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                .header(ADMIN_HEADER, ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalBrands").value(12))
            .andExpect(jsonPath("$.totalWallpapers").isNumber())
            .andExpect(jsonPath("$.pendingFeedback").isNumber());
    }

    @Test
    void shouldRefreshCatalogFromAdminApi() throws Exception {
        mockMvc.perform(post("/api/admin/catalog/refresh")
                .header(ADMIN_HEADER, ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalBrands").value(12))
            .andExpect(jsonPath("$.brands[0].slug").value("benz"));
    }

    @Test
    void shouldUpdateWallpaperMetadata() throws Exception {
        WallpaperEntity wallpaper = wallpaperRepository.findAllForAdmin().get(0);

        mockMvc.perform(patch("/api/admin/wallpapers/{wallpaperId}", wallpaper.getId())
                .header(ADMIN_HEADER, ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Admin Updated Wallpaper\",\"active\":false,\"sortOrder\":99}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(wallpaper.getId()))
            .andExpect(jsonPath("$.title").value("Admin Updated Wallpaper"))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.sortOrder").value(99));
    }

    @Test
    void shouldUploadAndDeleteWallpaper() throws Exception {
        BrandEntity brand = brandRepository.findBySlugIgnoreCase("benz").orElseThrow(IllegalStateException::new);

        MockMultipartFile originalFile = new MockMultipartFile(
            "file",
            "admin-upload-crud.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "original-binary".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile previewFile = new MockMultipartFile(
            "previewFile",
            "admin-upload-crud.webp",
            "image/webp",
            "preview-binary".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/brands/{brandId}/wallpapers", brand.getId())
                .file(originalFile)
                .file(previewFile)
                .param("title", "Admin Uploaded Wallpaper")
                .param("sortOrder", "250")
                .param("active", "true")
                .header(ADMIN_HEADER, ADMIN_KEY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.brandSlug").value("benz"))
            .andExpect(jsonPath("$.title").value("Admin Uploaded Wallpaper"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();

        JsonNode uploadedWallpaper = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        long wallpaperId = uploadedWallpaper.get("id").asLong();
        String storedFileName = uploadedWallpaper.get("fileName").asText();
        String previewFileName = decodeTrailingPathSegment(uploadedWallpaper.get("previewUrl").asText());

        org.junit.jupiter.api.Assertions.assertTrue(
            Files.exists(TEST_CATALOG_ROOT.resolve("MercedesBenz").resolve(storedFileName))
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            Files.exists(TEST_CATALOG_ROOT.resolve("_thumb").resolve("MercedesBenz").resolve(previewFileName))
        );

        mockMvc.perform(delete("/api/admin/wallpapers/{wallpaperId}", wallpaperId)
                .header(ADMIN_HEADER, ADMIN_KEY))
            .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(
            Files.exists(TEST_CATALOG_ROOT.resolve("MercedesBenz").resolve(storedFileName))
        );
        org.junit.jupiter.api.Assertions.assertFalse(
            Files.exists(TEST_CATALOG_ROOT.resolve("_thumb").resolve("MercedesBenz").resolve(previewFileName))
        );
    }

    @Test
    void shouldModerateFeedback() throws Exception {
        FeedbackMessage message = new FeedbackMessage();
        message.setName("Admin Review");
        message.setEmail("admin-review@example.com");
        message.setMessage("Pending feedback waiting for moderation.");
        message.setStatus(FeedbackStatus.PENDING);
        message.setFeatured(false);
        message.setSourcePage("/main.html");
        message.setUserAgent("MockMvc-Test");
        message = feedbackRepository.save(message);

        mockMvc.perform(patch("/api/admin/feedback/{feedbackId}", message.getId())
                .header(ADMIN_HEADER, ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"rejected\",\"featured\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(message.getId()))
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.featured").value(true));
    }

    @Test
    void shouldRejectUnsupportedFeedbackStatusFilter() throws Exception {
        mockMvc.perform(get("/api/admin/feedback")
                .header(ADMIN_HEADER, ADMIN_KEY)
                .param("status", "archived"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unsupported feedback status: archived"));
    }

    @Test
    void shouldRejectAdminRequestWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("缺少有效的管理员登录凭证。"));
    }

    @Test
    void shouldLoginWithAdminUsernameAndPassword() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-admin\",\"password\":\"test-admin-password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("test-admin"))
            .andExpect(jsonPath("$.authMode").value("PASSWORD"))
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void shouldExposeDashboardMetricsWithBearerToken() throws Exception {
        String token = loginAndExtractAccessToken();

        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalBrands").value(12))
            .andExpect(jsonPath("$.totalWallpapers").isNumber());
    }

    @Test
    void shouldRejectInvalidAdminLogin() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-admin\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("管理员账号或密码错误。"));
    }

    private String loginAndExtractAccessToken() throws Exception {
        String responseBody = mockMvc.perform(post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-admin\",\"password\":\"test-admin-password\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode payload = objectMapper.readTree(responseBody);
        return payload.get("accessToken").asText();
    }

    private static String decodeTrailingPathSegment(String path) {
        int slashIndex = path.lastIndexOf('/');
        String encodedSegment = slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
        try {
            return URLDecoder.decode(encodedSegment, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new IllegalStateException("Failed to decode uploaded preview file name.", exception);
        }
    }

    private static Path createTestCatalogRoot() {
        try {
            Path sourceRoot = Paths.get("..", "cars").toAbsolutePath().normalize();
            Path targetRoot = Files.createTempDirectory("vehicle-wallpaper-admin-catalog-");

            try (Stream<Path> sourcePaths = Files.walk(sourceRoot)) {
                sourcePaths.forEach(sourcePath -> {
                    Path targetPath = targetRoot.resolve(sourceRoot.relativize(sourcePath).toString());
                    try {
                        if (Files.isDirectory(sourcePath)) {
                            Files.createDirectories(targetPath);
                        } else {
                            Files.createDirectories(targetPath.getParent());
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to create test catalog copy.", exception);
                    }
                });
            }

            return targetRoot;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare the test catalog root.", exception);
        }
    }
}
