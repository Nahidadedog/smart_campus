package com.huadi.campus.vo;

import com.huadi.campus.entity.User;
import com.huadi.campus.enums.UserRole;

/** 登录成功后返回给前端的会话信息 */
public record LoginVO(String token, Long userId, String username, String realName, UserRole role) {

    public static LoginVO from(User user, String token) {
        return new LoginVO(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }
}
