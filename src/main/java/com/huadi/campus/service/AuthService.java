package com.huadi.campus.service;

import com.huadi.campus.dto.request.LoginRequest;
import com.huadi.campus.dto.request.RegisterRequest;
import com.huadi.campus.entity.Student;
import com.huadi.campus.entity.User;
import com.huadi.campus.enums.UserRole;
import com.huadi.campus.exception.ConflictException;
import com.huadi.campus.exception.UnauthorizedException;
import com.huadi.campus.repository.StudentRepository;
import com.huadi.campus.repository.UserRepository;
import com.huadi.campus.security.JwtService;
import com.huadi.campus.security.UserPrincipal;
import com.huadi.campus.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    /** 走标准 Spring Security 认证链（UserDetailsService + BCrypt），失败统一返回 401 且不区分用户名/密码错误 */
    @Transactional(readOnly = true)
    public LoginVO login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("用户不存在"));
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginVO.from(user, token);
    }

    /** 仅支持学生自助注册；教师 / 领导 / 管理员账号由管理员维护 */
    @Transactional
    public LoginVO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("用户名已存在");
        }
        if (studentRepository.existsByStudentNo(request.studentNo())) {
            throw new ConflictException("学号已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setRole(UserRole.STUDENT);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentNo(request.studentNo());
        student.setDepartment(request.department());
        student.setMajor(request.major());
        student.setClassName(request.className());
        student.setEnrollmentYear(request.enrollmentYear());
        studentRepository.save(student);

        return LoginVO.from(user, jwtService.generateToken(user.getId(), user.getUsername(), user.getRole()));
    }
}
