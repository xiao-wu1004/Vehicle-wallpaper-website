package com.vehiclewallpaper.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerTest {

    private static final String VISITOR_HEADER = "X-Visitor-Key";
    private static final String VISITOR_KEY = "visitor-demo-20260602";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExposeCatalogOverview() throws Exception {
        mockMvc.perform(get("/api/catalog")
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalBrands").value(12))
            .andExpect(jsonPath("$.brands[0].slug").value("benz"))
            .andExpect(jsonPath("$.brands[0].wallpapers[0].wallpaperId").isNumber())
            .andExpect(jsonPath("$.trendingWallpapers").isArray())
            .andExpect(jsonPath("$.totalFavorites").isNumber())
            .andExpect(jsonPath("$.totalDownloads").isNumber());
    }

    @Test
    void shouldTrackFavoritesDownloadsAndUserProfile() throws Exception {
        MvcResult overview = mockMvc.perform(get("/api/catalog")
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode overviewJson = objectMapper.readTree(overview.getResponse().getContentAsString());
        String wallpaperSlug = overviewJson.get("brands").get(0).get("wallpapers").get(0).get("id").asText();

        mockMvc.perform(post("/api/catalog/wallpapers/{wallpaperSlug}/favorite", wallpaperSlug)
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wallpaperId").value(wallpaperSlug))
            .andExpect(jsonPath("$.favorited").value(true))
            .andExpect(jsonPath("$.favoriteCount").value(1));

        mockMvc.perform(post("/api/catalog/wallpapers/{wallpaperSlug}/downloads", wallpaperSlug)
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wallpaperId").value(wallpaperSlug))
            .andExpect(jsonPath("$.downloadCount").value(1));

        mockMvc.perform(post("/api/catalog/wallpapers/{wallpaperSlug}/downloads", wallpaperSlug)
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wallpaperId").value(wallpaperSlug))
            .andExpect(jsonPath("$.downloadCount").value(2));

        mockMvc.perform(get("/api/catalog/me")
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visitorKey").value(VISITOR_KEY))
            .andExpect(jsonPath("$.favoriteCount").value(1))
            .andExpect(jsonPath("$.downloadCount").value(2))
            .andExpect(jsonPath("$.favorites[0].id").value(wallpaperSlug))
            .andExpect(jsonPath("$.recentDownloads[0].id").value(wallpaperSlug))
            .andExpect(jsonPath("$.recentDownloads.length()").value(1));

        mockMvc.perform(get("/api/catalog/wallpapers")
                .header(VISITOR_HEADER, VISITOR_KEY)
                .param("favoritesOnly", "true")
                .param("sort", "favorites"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(wallpaperSlug))
            .andExpect(jsonPath("$[0].favorited").value(true));

        mockMvc.perform(delete("/api/catalog/wallpapers/{wallpaperSlug}/favorite", wallpaperSlug)
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.favorited").value(false))
            .andExpect(jsonPath("$.favoriteCount").value(0));
    }

    @Test
    void shouldRejectFavoriteMutationWithoutVisitorKey() throws Exception {
        mockMvc.perform(post("/api/catalog/wallpapers/{wallpaperSlug}/favorite", "benz-1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Visitor key is required for this action."));
    }
}
