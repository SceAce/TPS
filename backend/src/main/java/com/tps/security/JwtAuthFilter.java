package com.tps.security;

/**
 * 文件说明：JWT 鉴权过滤器，负责从请求头解析令牌并建立当前用户身份。
 */

import com.tps.entity.User;
import com.tps.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isTokenValid(token) && !"refresh".equals(jwtUtil.getType(token))) {
                Long userId = jwtUtil.getUserId(token);
                String role = jwtUtil.getRole(token);
                userRepository.findById(userId).ifPresent(user -> {
                    // 只有状态正常的账号才允许进入安全上下文，避免被封禁账号拿着旧 token 继续访问。
                    if (user.getStatus() == User.UserStatus.ACTIVE) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                userId, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}
