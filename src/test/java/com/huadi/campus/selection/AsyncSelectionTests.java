package com.huadi.campus.selection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 进程内线程池异步落库（不依赖任何中间件）：
 * 接口先返回 PENDING，记录由 selection-persist 线程写入，"我的选课"稍后才能看到。
 */
@SpringBootTest(properties = "app.selection.persist=in-process")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AsyncSelectionTests {

    private static final long CS501 = 6;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void selectionReturnsPendingThenBecomesVisible() throws Exception {
        String token = login("student2");

        mockMvc.perform(post("/api/student/courses/" + CS501).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.courseId").value(CS501));

        // 异步落库：最终课程计数 +1，且出现在"我的选课"里
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(currentStudents(CS501)).isEqualTo(1));
        mockMvc.perform(get("/api/student/courses").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data[?(@.course.id == %d)]", CS501).exists());

        mockMvc.perform(delete("/api/student/courses/" + CS501).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(currentStudents(CS501)).isEqualTo(0));
    }

    private int currentStudents(long courseId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/courses/" + courseId)).andReturn();
        return json(result).path("data").path("currentStudents").asInt();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"123456\"}".formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data").path("token").asText();
    }

    private JsonNode json(MvcResult result) throws IOException {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
