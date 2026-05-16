package com.weeklyreport.controller;

import com.weeklyreport.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/settings")
    public ResponseEntity<SettingsService.SettingsData> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(
            @RequestBody SettingsService.SettingsData body) {
        boolean dbChanged = settingsService.updateSettings(body);

        String message;
        if (dbChanged) {
            message = "设置已保存。数据库路径已变更，请重启应用使其生效。";
        } else {
            message = "设置已保存。" + (body.getApiKey() != null && !body.getApiKey().isBlank()
                ? " API Key 已立即生效。" : "");
        }

        return ResponseEntity.ok(Map.of("success", true, "message", message,
            "restartRequired", dbChanged));
    }
}
