package com.vehiclewallpaper.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void shouldExposeCatalogSummaryForGuests() throws Exception {
        mockMvc.perform(get("/api/catalog/summary")
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalBrands").value(12))
            .andExpect(jsonPath("$.brands[0].slug").value("benz"))
            .andExpect(jsonPath("$.brands[0].displayName").isString())
            .andExpect(jsonPath("$.brands[0].wallpaperCount").isNumber())
            .andExpect(jsonPath("$.brands[0].coverImageUrl").isString())
            .andExpect(jsonPath("$.brands[0].wallpapers").doesNotExist())
            .andExpect(jsonPath("$.trendingWallpapers").isArray());
    }

    @Test
    void shouldTrackFavoritesDownloadsAndUserProfile() throws Exception {
        MvcResult overview = mockMvc.perform(get("/api/catalog")
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode overviewJson = objectMapper.readTree(overview.getResponse().getContentAsString());
        JsonNode initialWallpaper = overviewJson.get("brands").get(0).get("wallpapers").get(0);
        String wallpaperSlug = initialWallpaper.get("id").asText();
        int initialFavoriteCount = initialWallpaper.get("favoriteCount").asInt();
        int initialDownloadCount = initialWallpaper.get("downloadCount").asInt();

        mockMvc.perform(post("/api/catalog/wallpapers/{wallpaperSlug}/favorite", wallpaperSlug)
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wallpaperId").value(wallpaperSlug))
            .andExpect(jsonPath("$.favorited").value(true))
            .andExpect(jsonPath("$.favoriteCount").value(initialFavoriteCount + 1));

        mockMvc.perform(post("/api/catalog/wallpapers/{wallpaperSlug}/downloads", wallpaperSlug)
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wallpaperId").value(wallpaperSlug))
            .andExpect(jsonPath("$.downloadCount").value(initialDownloadCount + 1));

        mockMvc.perform(post("/api/catalog/wallpapers/{wallpaperSlug}/downloads", wallpaperSlug)
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wallpaperId").value(wallpaperSlug))
            .andExpect(jsonPath("$.downloadCount").value(initialDownloadCount + 2));

        mockMvc.perform(get("/api/catalog")
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.brands[0].wallpapers[0].id").value(wallpaperSlug))
            .andExpect(jsonPath("$.brands[0].wallpapers[0].favorited").value(true))
            .andExpect(jsonPath("$.brands[0].wallpapers[0].favoriteCount").value(initialFavoriteCount + 1))
            .andExpect(jsonPath("$.brands[0].wallpapers[0].downloadCount").value(initialDownloadCount + 2));

        JsonNode summaryJson = objectMapper.readTree(mockMvc.perform(get("/api/catalog/summary")
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());

        assertFalse(summaryJson.get("brands").get(0).has("wallpapers"));
        JsonNode summaryWallpaper = findWallpaperById(summaryJson.get("trendingWallpapers"), wallpaperSlug);
        assertNotNull(summaryWallpaper);
        assertEquals(true, summaryWallpaper.get("favorited").asBoolean());
        assertEquals(initialFavoriteCount + 1, summaryWallpaper.get("favoriteCount").asInt());
        assertEquals(initialDownloadCount + 2, summaryWallpaper.get("downloadCount").asInt());

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
            .andExpect(jsonPath("$.favoriteCount").value(initialFavoriteCount));

        mockMvc.perform(get("/api/catalog")
                .header(VISITOR_HEADER, VISITOR_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.brands[0].wallpapers[0].id").value(wallpaperSlug))
            .andExpect(jsonPath("$.brands[0].wallpapers[0].favorited").value(false))
            .andExpect(jsonPath("$.brands[0].wallpapers[0].favoriteCount").value(initialFavoriteCount))
            .andExpect(jsonPath("$.brands[0].wallpapers[0].downloadCount").value(initialDownloadCount + 2));
    }

    @Test
    void shouldBackfillWallpaperMetricsRowsForExistingWallpapers() {
        Integer wallpaperCount = jdbcTemplate.queryForObject("select count(*) from wallpapers", Integer.class);
        Integer metricsCount = jdbcTemplate.queryForObject("select count(*) from wallpaper_metrics", Integer.class);
        Long favoriteCount = jdbcTemplate.queryForObject("select coalesce(sum(favorite_count), 0) from wallpaper_metrics", Long.class);
        Long downloadCount = jdbcTemplate.queryForObject("select coalesce(sum(download_count), 0) from wallpaper_metrics", Long.class);
        Long favoriteEvents = jdbcTemplate.queryForObject("select count(*) from wallpaper_favorites", Long.class);
        Long downloadEvents = jdbcTemplate.queryForObject("select count(*) from wallpaper_download_events", Long.class);

        assertEquals(wallpaperCount, metricsCount);
        assertEquals(favoriteEvents, favoriteCount);
        assertEquals(downloadEvents, downloadCount);
    }

    @Test
    void shouldRejectFavoriteMutationWithoutVisitorKey() throws Exception {
        mockMvc.perform(post("/api/catalog/wallpapers/{wallpaperSlug}/favorite", "benz-1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Visitor key is required for this action."));
    }

    private JsonNode findWallpaperById(JsonNode wallpapers, String wallpaperId) {
        if (wallpapers == null || !wallpapers.isArray()) {
            return null;
        }

        for (JsonNode wallpaper : wallpapers) {
            if (wallpaperId.equals(wallpaper.path("id").asText())) {
                return wallpaper;
            }
        }
        return null;
    }
}
