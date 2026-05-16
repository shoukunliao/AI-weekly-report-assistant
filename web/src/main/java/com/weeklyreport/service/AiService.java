package com.weeklyreport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weeklyreport.config.DeepSeekConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final RestTemplate restTemplate;
    private final DeepSeekConfig config;
    private final ObjectMapper objectMapper;

    public AiService(RestTemplate restTemplate, DeepSeekConfig config, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public String polishReport(List<String> workItems, String weekRange) {
        String items = String.join("\n", workItems);
        String prompt = buildPolishPrompt(items, weekRange);

        return callDeepSeek(prompt);
    }

    public String classifyWork(String content) {
        String prompt = String.format(
            "请将以下工作内容归类到以下类别之一：开发、会议、文档、沟通、其他。只回复类别名称，不要解释。\n\n工作内容：%s", content
        );
        String result = callDeepSeek(prompt);
        return result != null ? result.trim() : "其他";
    }

    private String buildPolishPrompt(String items, String weekRange) {
        return String.format("""
            你是一个专业的周报撰写助手。请根据以下工作记录，生成一份结构化的周报。

            要求：
            1. 按类别分组（开发、会议、文档、沟通、其他）
            2. 合并相似的工作项，去除重复内容
            3. 使用专业但自然的职场语言，保留具体细节和数据
            4. 每个要点不超过两行
            5. 输出格式为 Markdown，以"## %s 周报"开头

            本周工作记录：
            %s
            """, weekRange, items);
    }

    private String callDeepSeek(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7,
                "max_tokens", 2000
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                config.getApiUrl(), request, String.class
            );

            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("choices").get(0)
                           .path("message").path("content").asText();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
