-- V3：课程增加选课时间窗口。
-- 名额预占前先在本地缓存里判断这两列，不在窗口内的请求连 Redis 都不用碰。
-- 两列为空表示不限制，兼容 V2 种子数据。
ALTER TABLE course ADD COLUMN enroll_start_time TIMESTAMP NULL;
ALTER TABLE course ADD COLUMN enroll_end_time   TIMESTAMP NULL;
