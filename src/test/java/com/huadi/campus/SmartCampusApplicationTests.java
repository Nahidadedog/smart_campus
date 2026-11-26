package com.huadi.campus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 上下文能否正常启动（数据源、JPA、Security、属性绑定全部就位） */
@SpringBootTest
@ActiveProfiles("test")
class SmartCampusApplicationTests {

    @Test
    void contextLoads() {
    }
}
