package com.vehiclewallpaper.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterLoginAndLogoutPublicUser() throws Exception {
        String email = "member-auth@example.com";
        String token = registerAndExtractAccessToken("Member Auth", email, "member-auth-password");

        mockMvc.perform(get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.displayName").value("Member Auth"))
            .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(false));

        mockMvc.perform(get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void shouldRejectInvalidPublicUserLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing-user@example.com\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Incorrect email or password."));
    }

    @Test
    void shouldUseAuthenticatedUserForCatalogFavoritesAndProfile() throws Exception {
        String token = registerAndExtractAccessToken("Garage Driver", "garage-driver@example.com", "garage-driver-password");

        MvcResult overview = mockMvc.perform(get("/api/catalog")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode overviewJson = objectMapper.readTree(overview.getResponse().getContentAsString());
        String wallpaperSlug = overviewJson.get("brands").get(0).get("wallpapers").get(0).get("id").asText();

        mockMvc.perform(post("/api/catalog/wallpapers/{wallpaperSlug}/favorite", wallpaperSlug)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wallpaperId").value(wallpaperSlug))
            .andExpect(jsonPath("$.favorited").value(true))
            .andExpect(jsonPath("$.favoriteCount").isNumber());

        mockMvc.perform(post("/api/catalog/wallpapers/{wallpaperSlug}/downloads", wallpaperSlug)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.downloadCount").isNumber());

        mockMvc.perform(get("/api/catalog/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.displayName").value("Garage Driver"))
            .andExpect(jsonPath("$.email").value("garage-driver@example.com"))
            .andExpect(jsonPath("$.favoriteCount").value(1))
            .andExpect(jsonPath("$.downloadCount").value(1))
            .andExpect(jsonPath("$.favorites[0].id").value(wallpaperSlug))
            .andExpect(jsonPath("$.recentDownloads[0].id").value(wallpaperSlug));

        mockMvc.perform(get("/api/catalog/wallpapers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("favoritesOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(wallpaperSlug))
            .andExpect(jsonPath("$[0].favorited").value(true));
    }

    private String registerAndExtractAccessToken(String displayName, String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"" + displayName + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.displayName").value(displayName))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.accessToken").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode payload = objectMapper.readTree(responseBody);
        return payload.get("accessToken").asText();
    }
}
