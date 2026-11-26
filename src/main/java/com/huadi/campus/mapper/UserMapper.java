package com.huadi.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huadi.campus.entity.User;

import java.util.Optional;

public interface UserMapper extends BaseMapper<User> {

    default Optional<User> selectByUsername(String username) {
        return Optional.ofNullable(selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)));
    }

    default boolean existsByUsername(String username) {
        return selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)) > 0;
    }
}
