package com.huadi.campus.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 * 分页插件会拦截 StatementHandler，改写原 SQL 追加 LIMIT，并额外发一条 COUNT 查询；
 * 不指定 DbType 时按 JDBC URL 自动识别（dev 是 H2，prod 是 MySQL）。
 */
@Configuration
@MapperScan("com.huadi.campus.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 不指定 DbType，启动后按 JDBC URL 自动识别（dev 是 H2，prod 是 MySQL）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
