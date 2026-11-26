package com.huadi.campus.vo;

/**
 * 选课接口的返回：
 * <ul>
 *   <li>SUCCESS：记录已写入数据库（同步模式）</li>
 *   <li>PENDING：名额已预占，记录由线程池 / MQ 消费者异步写入，前端稍后刷新"我的选课"</li>
 * </ul>
 * remaining 为预占后的剩余名额（缓存不可用时为 null）。
 */
public record SelectionResultVO(Long courseId, String status, Integer remaining) {

    public static final String SUCCESS = "SUCCESS";
    public static final String PENDING = "PENDING";
}
