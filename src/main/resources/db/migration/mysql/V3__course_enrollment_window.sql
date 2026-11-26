-- V3：课程增加选课时间窗口（与 h2/V3 对应，仅方言不同）。
ALTER TABLE course
    ADD COLUMN enroll_start_time DATETIME NULL COMMENT '选课开始时间，NULL 表示不限制',
    ADD COLUMN enroll_end_time   DATETIME NULL COMMENT '选课截止时间，NULL 表示不限制';
