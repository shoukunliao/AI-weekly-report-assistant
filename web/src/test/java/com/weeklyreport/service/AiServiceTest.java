package com.weeklyreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weeklyreport.config.DeepSeekConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private DeepSeekConfig config;
    private ObjectMapper objectMapper;
    private AiService aiService;

    @BeforeEach
    void setUp() {
        config = new DeepSeekConfig();
        config.setApiKey("test-key");
        config.setApiUrl("https://api.deepseek.com/v1/chat/completions");
        config.setModel("deepseek-chat");
        objectMapper = new ObjectMapper();
        aiService = new AiService(restTemplate, config, objectMapper);
    }

    @Test
    @DisplayName("classifyWork — API 返回有效类别时应正确返回")
    void classifyWork_shouldReturnCategory_whenApiReturnsValidResponse() throws Exception {
        mockApiResponse("开发");

        String result = aiService.classifyWork("修复了登录页面的bug");

        assertEquals("开发", result);
    }

    @Test
    @DisplayName("classifyWork — API 返回带空格的类别时应 trim")
    void classifyWork_shouldTrimWhitespace() throws Exception {
        mockApiResponse("  文档  ");

        String result = aiService.classifyWork("编写了接口文档");

        assertEquals("文档", result);
    }

    @Test
    @DisplayName("classifyWork — API 异常时应降级返回\"其他\"")
    void classifyWork_shouldReturnDefault_whenApiFails() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        String result = aiService.classifyWork("做了一些工作");

        assertEquals("其他", result);
    }

    @Test
    @DisplayName("classifyWork — API 返回空 body 时应降级返回\"其他\"")
    void classifyWork_shouldReturnDefault_whenBodyIsNull() {
        ResponseEntity<String> response = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(response);

        String result = aiService.classifyWork("做了一些工作");

        assertEquals("其他", result);
    }

    @Test
    @DisplayName("polishReport — API 返回正常内容时应正确返回")
    void polishReport_shouldReturnPolishedContent_whenApiReturnsValidResponse() throws Exception {
        String polished = "## 第20周 周报\n\n### 开发\n- 修复登录页面bug\n- 完成用户模块重构";
        mockApiResponse(polished);

        String result = aiService.polishReport(
                List.of("修复了登录页面的bug", "完成了用户模块的重构"),
                "第20周"
        );

        assertEquals(polished, result);
    }

    @Test
    @DisplayName("polishReport — API 异常时应返回 null")
    void polishReport_shouldReturnNull_whenApiFails() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Timeout"));

        String result = aiService.polishReport(
                List.of("修复了登录页面的bug"),
                "第20周"
        );

        assertNull(result);
    }

    // ---- helpers ----

    private void mockApiResponse(String content) throws Exception {
        String respBody = objectMapper.writeValueAsString(
                Map.of("choices", List.of(
                        Map.of("message", Map.of("content", content))
                ))
        );
        ResponseEntity<String> response = new ResponseEntity<>(respBody, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(response);
    }
}
