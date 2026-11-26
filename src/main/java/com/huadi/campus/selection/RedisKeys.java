package com.huadi.campus.selection;

/**
 * Redis key 命名统一管理，避免各处手写字符串拼接。
 *
 * @param prefix 前缀，默认 campus，多环境共用 Redis 时用来隔离
 */
public record RedisKeys(String prefix) {

    /** 剩余名额（String，整数） */
    public String stock(Long courseId) {
        return prefix + ":course:stock:" + courseId;
    }

    /** 已预占 / 已选该课程的学生集合（Set，studentId） */
    public String selected(Long courseId) {
        return prefix + ":course:selected:" + courseId;
    }

    /** 懒加载某门课时的互斥锁 */
    public String loadLock(Long courseId) {
        return prefix + ":course:lock:" + courseId;
    }

    /** 限流桶 */
    public String rateLimit(String key) {
        return prefix + ":rl:" + key;
    }
}
