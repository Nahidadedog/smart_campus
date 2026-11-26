package com.huadi.campus.security;

import com.huadi.campus.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Spring Security 中的当前用户。既用于登录认证，也作为 JWT 过滤器重建的 Principal。 */
public record UserPrincipal(Long id, String username, String password, UserRole role, boolean enabled)
        implements UserDetails {

    /** JWT 过滤器使用：只携带 token 中的信息，不含密码 */
    public static UserPrincipal fromToken(Long id, String username, UserRole role) {
        return new UserPrincipal(id, username, null, role, true);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
