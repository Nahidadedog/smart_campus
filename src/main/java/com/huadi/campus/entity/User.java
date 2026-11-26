package com.huadi.campus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.huadi.campus.enums.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 登录用户。与 Student、Teacher 等业务表通过 user_id 一对一关联 */
@Getter
@Setter
@NoArgsConstructor
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码哈希：禁止序列化输出，接口统一通过 VO 返回 */
    @JsonIgnore
    private String password;

    private String realName;
    private String phone;
    private String email;
    private String gender;
    private String avatar;

    /** 数据库与 JWT 中均以枚举名存储 */
    private UserRole role;

    private Boolean enabled = true;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
