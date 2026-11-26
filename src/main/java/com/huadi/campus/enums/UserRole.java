package com.huadi.campus.enums;

/** 用户角色。数据库与 JWT 中均以枚举名存储，Spring Security 权限为 ROLE_ 前缀。 */
public enum UserRole {
    STUDENT, TEACHER, LEADER, ADMIN
}
