package com.vehiclewallpaper.backend;

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
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WallpaperRepository wallpaperRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void shouldExposeDashboardMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalBrands").value(12))
            .andExpect(jsonPath("$.totalWallpapers").isNumber())
            .andExpect(jsonPath("$.pendingFeedback").isNumber());
    }

    @Test
    void shouldRefreshCatalogFromAdminApi() throws Exception {
        mockMvc.perform(post("/api/admin/catalog/refresh"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalBrands").value(12))
            .andExpect(jsonPath("$.brands[0].slug").value("benz"));
    }

    @Test
    void shouldUpdateWallpaperMetadata() throws Exception {
        WallpaperEntity wallpaper = wallpaperRepository.findAllForAdmin().get(0);

        mockMvc.perform(patch("/api/admin/wallpapers/{wallpaperId}", wallpaper.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Admin Updated Wallpaper\",\"active\":false,\"sortOrder\":99}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(wallpaper.getId()))
            .andExpect(jsonPath("$.title").value("Admin Updated Wallpaper"))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.sortOrder").value(99));
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
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"rejected\",\"featured\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(message.getId()))
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.featured").value(true));
    }

    @Test
    void shouldRejectUnsupportedFeedbackStatusFilter() throws Exception {
        mockMvc.perform(get("/api/admin/feedback").param("status", "archived"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unsupported feedback status: archived"));
    }
}
