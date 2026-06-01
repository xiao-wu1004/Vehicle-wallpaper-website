package com.vehiclewallpaper.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAcceptFeedbackSubmission() throws Exception {
        String payload = "{\"name\":\"测试用户\",\"email\":\"test@example.com\",\"message\":\"这个站点的壁纸质量很高。\",\"page\":\"/main.html\"}";

        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldExposeSeededFeedbackHighlights() throws Exception {
        mockMvc.perform(get("/api/feedback/highlights"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").exists());
    }
}
