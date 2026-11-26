package com.huadi.campus.selection;

/** 一次名额预占的结果。 */
public enum ReserveResult {

    /** 预占成功，可以进入落库环节 */
    ACQUIRED,
    /** 该学生已选（或上一次请求仍在异步落库中） */
    DUPLICATE,
    /** 名额已满 */
    SOLD_OUT,
    /** 课程不存在 */
    NOT_FOUND,
    /** 不在选课时间窗口内 */
    NOT_OPEN
}
