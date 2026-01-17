package com.tamakara.bakabooru.module.system.service;

import com.tamakara.bakabooru.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SystemSettingService systemSettingService;

    public boolean isPasswordSet() {
        String password = systemSettingService.getSetting("auth.password", "");
        return StringUtils.hasText(password);
    }

    public boolean isInitialized() {
        return systemSettingService.getBooleanSetting("auth.initialized", false);
    }

    public String login(String password) {
        String storedEncodedPassword = systemSettingService.getSetting("auth.password", "");
        String currentPassword = "";

        if (StringUtils.hasText(storedEncodedPassword)) {
            try {
                currentPassword = new String(Base64.getDecoder().decode(storedEncodedPassword), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                // If decoding fails, treat as empty or mismatch
                currentPassword = "";
            }
        }

        // If no password set (empty), allow empty login?
        // User said: "First visit prompt to set password, can also not set password (password is empty string)".
        // If they choose not to set password, stored is "" (encoded "").
        // If input matches current, generate token.

        if (!currentPassword.equals(password)) {
            throw new RuntimeException("密码错误");
        }

        // Generate token
        return JwtUtils.createToken(JwtUtils.generateSecretKey(currentPassword), 1000 * 60 * 60 * 24 * 7); // 7 days
    }

    public void setPassword(String password) {
        if (password == null) password = "";
        String encoded = Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
        Map<String, String> map = new HashMap<>();
        map.put("auth.password", encoded);
        map.put("auth.initialized", "true");
        systemSettingService.updateSettings(map);
    }

    public void validate(String token) {
        if (!isInitialized()) return; // Allow if not initialized (to run setup)? Or maybe strict?
        // Actually, if not initialized, we shouldn't be calling API other than setup.
        // But let's assume validate is for authorized endpoints.

        String storedEncodedPassword = systemSettingService.getSetting("auth.password", "");
        // If password is empty, storedEncodedPassword is ""

        String currentPassword = "";
        if (StringUtils.hasText(storedEncodedPassword)) {
            try {
                currentPassword = new String(Base64.getDecoder().decode(storedEncodedPassword), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                currentPassword = "";
            }
        }

        // If currentPassword is empty, does it mean NO AUTH required?
        // User says "can also not set password".
        // If so, token validation might be optional or trivial.
        // But if client sends token, we validate it.
        // If client doesn't send token?
        // Interceptor will call this only if token exists? Or always?
        // If password is empty, we should probably return void (valid)
        if (!StringUtils.hasText(currentPassword)) {
            return;
        }

        if (JwtUtils.isTokenExpired(token, JwtUtils.generateSecretKey(currentPassword))) {
            throw new RuntimeException("Token已过期 or 无效");
        }
    }
}
