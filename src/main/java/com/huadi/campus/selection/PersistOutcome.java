package com.huadi.campus.selection;

/** 落库结果 */
public enum PersistOutcome {
    /** 新增或复用记录成功 */
    PERSISTED,
    /** 数据库里已经是"已选"（并发重复提交），本次预留的名额需要还回去 */
    DUPLICATE
}
