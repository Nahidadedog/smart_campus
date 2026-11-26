package com.huadi.campus.selection;

/**
 * 选课落库指令：Redis 预占成功后产生，由某种持久化通道（同步 / 线程池 / MQ）交给消费者写数据库。
 * requestId 用于日志串联与 MQ 幂等跟踪。
 */
public record SelectionCommand(Long studentId, Long courseId, String requestId) {

    public static SelectionCommand of(Long studentId, Long courseId) {
        return new SelectionCommand(studentId, courseId, java.util.UUID.randomUUID().toString());
    }
}
