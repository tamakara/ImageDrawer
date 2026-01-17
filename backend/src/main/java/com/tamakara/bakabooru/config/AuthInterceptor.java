package com.tamakara.bakabooru.config;

import com.tamakara.bakabooru.module.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI();

        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/status") || path.startsWith("/api/auth/setup")) return true;

        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                authService.validate(token);
                return true;
            } catch (Exception e) {
                response.setStatus(401);
                return false;
            }
        }

        if (!authService.isInitialized()) {
            response.setStatus(401);
            return false;
        }

        if (authService.isPasswordSet()) {
            response.setStatus(401);
            return false;
        }

        return true;
    }
}

