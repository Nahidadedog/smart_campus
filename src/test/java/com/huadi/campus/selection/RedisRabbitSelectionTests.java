package com.huadi.campus.selection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadi.campus.entity.Course;
import com.huadi.campus.mapper.CourseMapper;
import com.huadi.campus.ratelimit.RedisRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 真实 Redis + RabbitMQ 容器下的端到端验证：
 * <ul>
 *   <li>并发抢课零超卖（Lua 原子性）</li>
 *   <li>接口先返回 PENDING，MQ 消费者落库后计数与列表最终一致</li>
 *   <li>落库失败（外键违例）→ 名额回补 Redis + 消息进入死信队列</li>
 *   <li>限流器：令牌耗尽返回 429，随时间补充</li>
 * </ul>
 * 没有 Docker 时整个类自动跳过。
 */
@SpringBootTest(properties = {
        "app.selection.stock=redis",
        "app.selection.persist=rabbit",
        "app.selection.rate-limit.enabled=true",
        "app.selection.rate-limit.capacity=2",
        "app.selection.rate-limit.refill-per-second=1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RedisRabbitSelectionTests {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static GenericContainer<?> rabbit = new GenericContainer<>(DockerImageName.parse("rabbitmq:3.13-management-alpine"))
            .withExposedPorts(5672);

    /** 容器映射到宿主机的随机端口在启动后才知道，所以用 DynamicPropertySource 注入 */
    @DynamicPropertySource
    static void middlewareProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbit.getMappedPort(5672));
    }

    private static final long CS501 = 6;
    private static final long CS601 = 7;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StockService stockService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedisKeys keys;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private RedisRateLimiter limiter;
    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private RabbitSelectionPublisher publisher;
    @Autowired
    private ConnectionFactory connectionFactory;

    private String student1;
    private String student2;
    private String student3;

    @BeforeEach
    void setUp() throws Exception {
        // 先确保到 Broker 的连接是活的：机器负载高时连接可能被心跳超时断开，
        // 否则第一个请求会走"投递失败 → 同步落库"的降级分支，返回 SUCCESS 而不是 PENDING
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(connectionFactory.createConnection().isOpen()).isTrue());
        student1 = login("student1");
        student2 = login("student2");
        student3 = login("student3");
    }

    // ---------- Lua 原子性 ----------

    @Test
    void concurrentReserveNeverOversells() throws Exception {
        Course course = newCourse("并发测试课", 20);
        long courseId = course.getId();
        stockService.warmUp();

        int threads = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ReserveResult>> futures = new java.util.ArrayList<>();
        for (int i = 1; i <= threads; i++) {
            long studentId = i;
            futures.add(pool.submit(() -> {
                start.await();
                return stockService.reserve(courseId, studentId);
            }));
        }
        start.countDown();
        int acquired = 0;
        int soldOut = 0;
        for (Future<ReserveResult> future : futures) {
            ReserveResult result = future.get(10, TimeUnit.SECONDS);
            if (result == ReserveResult.ACQUIRED) {
                acquired++;
            } else if (result == ReserveResult.SOLD_OUT) {
                soldOut++;
            }
        }
        pool.shutdown();

        assertThat(acquired).isEqualTo(20);
        assertThat(soldOut).isEqualTo(threads - 20);
        assertThat(redisTemplate.opsForValue().get(keys.stock(courseId))).isEqualTo("0");
    }

    @Test
    void duplicateIsRejectedAndReleaseRestoresSeat() throws Exception {
        Course course = newCourse("幂等测试课", 1);
        long courseId = course.getId();
        stockService.warmUp();

        assertThat(stockService.reserve(courseId, 101L)).isEqualTo(ReserveResult.ACQUIRED);
        assertThat(stockService.reserve(courseId, 101L)).isEqualTo(ReserveResult.DUPLICATE);
        assertThat(stockService.reserve(courseId, 102L)).isEqualTo(ReserveResult.SOLD_OUT);

        stockService.releaseReservation(new SelectionCommand(101L, courseId, "t"));
        assertThat(stockService.reserve(courseId, 102L)).isEqualTo(ReserveResult.ACQUIRED);
        // 重复释放不会多加名额
        stockService.releaseReservation(new SelectionCommand(101L, courseId, "t"));
        assertThat(redisTemplate.opsForValue().get(keys.stock(courseId))).isEqualTo("0");
    }

    @Test
    void missingCacheIsLazyLoadedFromDatabase() throws Exception {
        Course course = newCourse("懒加载测试课", 3);
        long courseId = course.getId();
        redisTemplate.delete(keys.stock(courseId));
        redisTemplate.delete(keys.selected(courseId));

        assertThat(stockService.reserve(courseId, 201L)).isEqualTo(ReserveResult.ACQUIRED);
        assertThat(redisTemplate.opsForValue().get(keys.stock(courseId))).isEqualTo("2");
    }

    // ---------- 接口端到端：Redis 预占 + MQ 落库 ----------

    @Test
    void apiReturnsPendingThenMqConsumerPersists() throws Exception {
        mockMvc.perform(post("/api/student/courses/" + CS501).header("Authorization", bearer(student1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.remaining").value(59));

        // 名额已在 Redis 扣减；数据库记录由消费者稍后写入
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(currentStudents(CS501)).isEqualTo(1));
        mockMvc.perform(get("/api/student/courses").header("Authorization", bearer(student1)))
                .andExpect(jsonPath("$.data[?(@.course.id == %d)]", CS501).exists());

        // 退选：事务提交后释放 Redis 名额
        mockMvc.perform(delete("/api/student/courses/" + CS501).header("Authorization", bearer(student1)))
                .andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get(keys.stock(CS501))).isEqualTo("60");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(currentStudents(CS501)).isEqualTo(0));
    }

    @Test
    void persistFailureGoesToDeadLetterQueueAndRestoresStock() throws Exception {
        Course course = newCourse("失败回补测试课", 5);
        long courseId = course.getId();
        stockService.warmUp();
        assertThat(stockService.reserve(courseId, 1L)).isEqualTo(ReserveResult.ACQUIRED);
        assertThat(redisTemplate.opsForValue().get(keys.stock(courseId))).isEqualTo("4");

        // 让落库必然失败：删掉课程，插入选课记录时外键违例
        courseMapper.deleteById(courseId);
        publisher.publish(new SelectionCommand(1L, courseId, "it-fail"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(redisTemplate.opsForValue().get(keys.stock(courseId))).isEqualTo("5");
            assertThat(amqpAdmin.getQueueInfo(RabbitSelectionConfig.DLQ).getMessageCount()).isEqualTo(1);
        });
    }

    // ---------- 限流 ----------

    @Test
    void rateLimitReturns429WhenTokensExhausted() throws Exception {
        // 容量 2：第一次选课成功、第二次重复选课（消耗令牌）、第三次被限流
        mockMvc.perform(post("/api/student/courses/" + CS601).header("Authorization", bearer(student2)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/student/courses/" + CS601).header("Authorization", bearer(student2)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/student/courses/" + CS601).header("Authorization", bearer(student2)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));

        // 清理：等消费者把第一次选课落库后退掉，避免影响其它用例
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(currentStudents(CS601)).isEqualTo(1));
        mockMvc.perform(delete("/api/student/courses/" + CS601).header("Authorization", bearer(student2)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenBucketRefillsOverTime() throws Exception {
        String key = "it-" + System.nanoTime();
        assertThat(limiter.tryAcquire(key, 2, 1.0)).isTrue();
        assertThat(limiter.tryAcquire(key, 2, 1.0)).isTrue();
        assertThat(limiter.tryAcquire(key, 2, 1.0)).isFalse();
        Thread.sleep(1200);
        assertThat(limiter.tryAcquire(key, 2, 1.0)).isTrue();
    }

    // ---------- helpers ----------

    private Course newCourse(String name, int maxStudents) {
        Course course = new Course();
        course.setName(name);
        course.setCode("IT-" + System.nanoTime() % 1000000);
        course.setCredits(1.0);
        course.setHours(16);
        course.setType("选修");
        course.setSemester("2025-2026-2");
        course.setMaxStudents(maxStudents);
        course.setCurrentStudents(0);
        courseMapper.insert(course);
        return course;
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
