package com.tamakara.bakabooru.module.system.service;

import com.tamakara.bakabooru.module.system.entity.SystemSetting;
import com.tamakara.bakabooru.module.system.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    // 将设置缓存在内存中
    private final Map<String, String> settingsCache = new HashMap<>();

    @PostConstruct
    public void init() {
        initDefaultSettings();
        refreshCache();
    }

    private void initDefaultSettings() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("upload.max-file-size", "5242880");
        defaults.put("upload.allowed-extensions", "jpg,png,webp,gif,jpeg");
        defaults.put("upload.concurrency", "3");
        defaults.put("upload.poll-interval", "1000");
        defaults.put("file.thumbnail.quality", "80");
        defaults.put("file.thumbnail.max-size", "800");
        defaults.put("tag.threshold", "0.6");
        defaults.put("auth.password", "");
        defaults.put("auth.initialized", "false");
        defaults.put("llm.url", "");
        defaults.put("llm.model", "");
        defaults.put("llm.api-key", "");

        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (!systemSettingRepository.existsById(entry.getKey())) {
                systemSettingRepository.save(new SystemSetting(entry.getKey(), entry.getValue()));
            }
        }
    }

    public void refreshCache() {
        List<SystemSetting> all = systemSettingRepository.findAll();
        settingsCache.clear();
        for (SystemSetting setting : all) {
            settingsCache.put(setting.getKey(), setting.getValue());
        }
    }

    public String getSetting(String key, String defaultValue) {
        return settingsCache.getOrDefault(key, defaultValue);
    }

    public Boolean getBooleanSetting(String key, Boolean defaultValue) {
        String val = settingsCache.get(key);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val);
    }

    public int getIntSetting(String key, int defaultValue) {
        String val = settingsCache.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLongSetting(String key, long defaultValue) {
        String val = settingsCache.get(key);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDoubleSetting(String key, double defaultValue) {
        String val = settingsCache.get(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            SystemSetting setting = new SystemSetting(entry.getKey(), entry.getValue());
            systemSettingRepository.save(setting);
            settingsCache.put(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, String> getAllSettings() {
        return new HashMap<>(settingsCache);
    }

    @Transactional
    public void resetSettings() {
        systemSettingRepository.deleteAll();
        init();
    }
}
