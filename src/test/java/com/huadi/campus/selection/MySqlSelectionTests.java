package com.huadi.campus.selection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 真实 MySQL 8 下的选课链路验证（H2 与 MySQL 的行为差异只在这里能暴露）：
 * <ul>
 *   <li>Flyway 的 mysql 方言脚本（建表 + 演示数据 + V3 加列）能完整执行</li>
 *   <li>同步落库：StudentService 类级 readOnly 事务必须被挂起，否则 MySQL 报 "Connection is read-only"</li>
 *   <li>选课时间窗口、管理员建课 / 改课</li>
 * </ul>
 * 没有 Docker 时整个类自动跳过。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MySqlSelectionTests {

    /** 官方 MySQLContainer 会等到 JDBC 真能连上才返回（首次初始化会先后起临时实例与正式实例，光看日志会误判） */
    @Container
    static MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("smart_campus")
            .withUsername("root")
            .withPassword("root");

    // withUrlParam 返回的是父类型 JdbcDatabaseContainer，单独调用以免丢失 MySQLContainer 类型
    static {
        mysql.withUrlParam("characterEncoding", "utf8").withUrlParam("serverTimezone", "Asia/Shanghai");
    }

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.driver-class-name", () -> mysql.getDriverClassName());
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    private static final long CS401 = 5;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void selectThenDropPersistsOnMySql() throws Exception {
        String token = login("student4");
        assertThat(currentStudents(CS401)).isZero();

        mockMvc.perform(post("/api/student/courses/" + CS401).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.remaining").value(39));
        assertThat(currentStudents(CS401)).isEqualTo(1);

        mockMvc.perform(delete("/api/student/courses/" + CS401).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        assertThat(currentStudents(CS401)).isZero();
    }

    @Test
    void enrollmentWindowBlocksSelectionUntilReopened() throws Exception {
        String admin = login("admin");
        String student = login("student4");
        String code = "MY-" + System.nanoTime() % 1000000;
        String start = LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String end = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        MvcResult created = mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"窗口测试课","code":"%s","credits":1,"hours":16,"type":"选修","teacherId":1,
                                 "semester":"2025-2026-2","schedule":"待定","classroom":"待定","maxStudents":30,
                                 "enrollStartTime":"%s","enrollEndTime":"%s"}
                                """.formatted(code, start, end)))
                .andExpect(status().isOk())
                .andReturn();
        long courseId = json(created).path("data").path("id").asLong();

        mockMvc.perform(post("/api/student/courses/" + courseId).header("Authorization", bearer(student)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("不在选课时间内"));

        // 管理员把窗口清空 = 不限制
        mockMvc.perform(put("/api/admin/courses/" + courseId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"窗口测试课","code":"%s","credits":1,"hours":16,"type":"选修","teacherId":1,
                                 "semester":"2025-2026-2","schedule":"待定","classroom":"待定","maxStudents":30}
                                """.formatted(code)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/student/courses/" + courseId).header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
        mockMvc.perform(delete("/api/student/courses/" + courseId).header("Authorization", bearer(student)))
                .andExpect(status().isOk());
    }

    @Test
    void contactUpdateIsWrittenOnMySql() throws Exception {
        String token = login("student4");
        mockMvc.perform(put("/api/student/profile/contact")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000004\",\"email\":\"student4@campus.edu.cn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("13900000004"));
    }

    // ---------- helpers ----------

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

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
