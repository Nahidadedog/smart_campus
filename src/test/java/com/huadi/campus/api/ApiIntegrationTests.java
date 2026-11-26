package com.huadi.campus.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口层集成测试：重点覆盖鉴权边界与本次重构修掉的几个问题
 * （匿名可写内容、注册提权、实体泄露密码、越权录成绩、重复选课/退选后再选）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String studentToken;
    private String teacherToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        studentToken = login("student1");
        teacherToken = login("teacher1");
        adminToken = login("admin");
    }

    // ---------- 认证 ----------

    @Test
    void loginReturnsTokenAndRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student1\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.realName").value("张三"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student1\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void loginValidationFailureReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void anonymousAccessReturns401Json() throws Exception {
        mockMvc.perform(get("/api/student/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void wrongRoleReturns403Json() throws Exception {
        mockMvc.perform(get("/api/teacher/profile").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ---------- 本次修复的安全问题 ----------

    @Test
    void anonymousCannotWriteContent() throws Exception {
        String body = "{\"title\":\"x\",\"content\":\"y\",\"type\":\"学校公告\",\"top\":false}";
        mockMvc.perform(post("/api/admin/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerIgnoresPrivilegedRoles() throws Exception {
        // 即使请求体里夹带 role=ADMIN，也只会创建 STUDENT
        String body = """
                {"username":"newbie_%d","password":"123456","realName":"新生","studentNo":"2023099",
                 "department":"计算机学院","major":"软件工程","enrollmentYear":"2023","role":"ADMIN"}
                """.formatted(System.nanoTime() % 100000);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andReturn();

        String token = json(result).path("data").path("token").asText();
        mockMvc.perform(get("/api/admin/announcements").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerWithDuplicateUsernameReturns409() throws Exception {
        String body = """
                {"username":"student1","password":"123456","realName":"重复","studentNo":"2023999",
                 "department":"计算机学院","enrollmentYear":"2023"}
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void profileNeverLeaksPasswordHash() throws Exception {
        mockMvc.perform(get("/api/student/profile").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentNo").value("2023001"))
                .andExpect(jsonPath("$.data.realName").value("张三"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void adminCanReadLeaderStatistics() throws Exception {
        mockMvc.perform(get("/api/leader/statistics/students").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").value(4))
                .andExpect(jsonPath("$.data.byDepartment.计算机科学与技术学院").value(3));
        mockMvc.perform(get("/api/leader/statistics/courses").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCourses").value(7))
                .andExpect(jsonPath("$.data.totalSelections").value(7));
    }

    // ---------- 业务规则 ----------

    @Test
    void studentCannotEnrollTwiceAndCanReEnrollAfterDropping() throws Exception {
        // student1 未选 CS401
        mockMvc.perform(post("/api/student/courses/5").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.course.code").value("CS401"))
                .andExpect(jsonPath("$.data.course.currentStudents").value(1));

        mockMvc.perform(post("/api/student/courses/5").header("Authorization", bearer(studentToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("已选择该课程"));

        mockMvc.perform(delete("/api/student/courses/5").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());

        // 退选后再选：复用原记录，不触发唯一约束
        mockMvc.perform(post("/api/student/courses/5").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());

        // 测试结束后清理，避免影响其他用例
        mockMvc.perform(delete("/api/student/courses/5").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCannotGradeCourseOfAnotherTeacher() throws Exception {
        // CS101 属于 teacher1，teacher2 无权
        String body = "{\"studentId\":1,\"score\":88,\"semester\":\"2025-2026-1\"}";
        mockMvc.perform(put("/api/teacher/courses/1/grades")
                        .header("Authorization", bearer(login("teacher2")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void savingGradeTwiceUpdatesSameRecord() throws Exception {
        String body = "{\"studentId\":1,\"score\":88,\"semester\":\"2025-2026-1\"}";

        MvcResult first = mockMvc.perform(put("/api/teacher/courses/1/grades")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gradeLevel").value("良"))
                .andReturn();
        long firstId = json(first).path("data").path("id").asLong();

        MvcResult second = mockMvc.perform(put("/api/teacher/courses/1/grades")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"score\":95,\"semester\":\"2025-2026-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gradeLevel").value("优"))
                .andReturn();
        assertThat(json(second).path("data").path("id").asLong()).isEqualTo(firstId);

        mockMvc.perform(get("/api/teacher/courses/1/grades").header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.semester == '2025-2026-1')]").isArray());
    }

    @Test
    void gradeValidationRejectsOutOfRangeScore() throws Exception {
        mockMvc.perform(put("/api/teacher/courses/1/grades")
                        .header("Authorization", bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"score\":120,\"semester\":\"2025-2026-1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicCatalogSearchWorks() throws Exception {
        mockMvc.perform(get("/api/public/courses").param("keyword", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("CS101"))
                .andExpect(jsonPath("$.data[0].teacherName").value("刘老师"))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void unknownResourceReturns404Json() throws Exception {
        mockMvc.perform(get("/api/public/news/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("新闻不存在"));
    }

    // ---------- helpers ----------

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
