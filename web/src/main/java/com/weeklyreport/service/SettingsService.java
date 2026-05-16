package com.weeklyreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.weeklyreport.config.DeepSeekConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class SettingsService {

    private static final String SETTINGS_FILE = "settings.json";

    private final ObjectMapper objectMapper;
    private final DeepSeekConfig deepSeekConfig;

    private String apiKey;
    private String dbPath;

    public SettingsService(ObjectMapper objectMapper, DeepSeekConfig deepSeekConfig) {
        this.objectMapper = objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.deepSeekConfig = deepSeekConfig;
    }

    @PostConstruct
    public void init() {
        loadFromFile();
        if (apiKey != null && !apiKey.isBlank()) {
            deepSeekConfig.setApiKey(apiKey);
        }
    }

    public static void applyOnStartup() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) return;
        try {
            SettingsData data = new ObjectMapper().readValue(file, SettingsData.class);
            if (data.getApiKey() != null && !data.getApiKey().isBlank()) {
                System.setProperty("weeklyreport.api-key", data.getApiKey());
            }
            if (data.getDbPath() != null && !data.getDbPath().isBlank()) {
                String url = data.getDbPath().startsWith("jdbc:sqlite:")
                    ? data.getDbPath()
                    : "jdbc:sqlite:" + data.getDbPath();
                System.setProperty("weeklyreport.db-path", url);
            }
        } catch (IOException e) {
            // settings file corrupted, ignore
        }
    }

    public SettingsData getSettings() {
        SettingsData data = new SettingsData();
        data.setApiKey(maskApiKey(apiKey));
        data.setDbPath(dbPath);
        return data;
    }

    public boolean updateSettings(SettingsData newSettings) {
        boolean dbChanged = false;

        if (newSettings.getApiKey() != null && !newSettings.getApiKey().isBlank()) {
            this.apiKey = newSettings.getApiKey();
            deepSeekConfig.setApiKey(apiKey);
        }

        if (newSettings.getDbPath() != null && !newSettings.getDbPath().isBlank()) {
            String oldPath = this.dbPath;
            this.dbPath = newSettings.getDbPath();
            if (oldPath == null || !oldPath.equals(dbPath)) {
                dbChanged = true;
            }
        }

        saveToFile();
        return dbChanged;
    }

    private void loadFromFile() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) return;
        try {
            SettingsData data = objectMapper.readValue(file, SettingsData.class);
            this.apiKey = data.getApiKey();
            this.dbPath = data.getDbPath();
        } catch (IOException e) {
            // ignore
        }
    }

    private void saveToFile() {
        try {
            SettingsData data = new SettingsData();
            data.setApiKey(apiKey);
            data.setDbPath(dbPath);
            objectMapper.writeValue(new File(SETTINGS_FILE), data);
        } catch (IOException e) {
            // ignore
        }
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() <= 8) return key;
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    public static class SettingsData {
        private String apiKey;
        private String dbPath;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getDbPath() { return dbPath; }
        public void setDbPath(String dbPath) { this.dbPath = dbPath; }
    }
}
