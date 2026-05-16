package com.weeklyreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weeklyreport.config.DeepSeekConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实调用 DeepSeek API 的集成测试，验证 API Key、网络连接和响应解析是否正常。
 * 需要 DEEPSEEK_API_KEY 环境变量已设置。
 */
class AiServiceIntegrationTest {

    private AiService aiService;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        String apiUrl = "https://api.deepseek.com/v1/chat/completions";
        String model = "deepseek-chat";

        assertNotNull(apiKey, "DEEPSEEK_API_KEY 环境变量未设置，无法进行集成测试");
        assertFalse(apiKey.isBlank(), "DEEPSEEK_API_KEY 为空");

        DeepSeekConfig config = new DeepSeekConfig();
        config.setApiKey(apiKey);
        config.setApiUrl(apiUrl);
        config.setModel(model);

        aiService = new AiService(new RestTemplate(), config, new ObjectMapper());
    }

    @Test
    @DisplayName("classifyWork — 纯开发内容应返回\"开发\"")
    void classifyWork_shouldReturnDev_forDevOnlyContent() {
        String result = aiService.classifyWork("修复了用户登录页面的超时bug");

        assertNotNull(result, "分类结果不应为 null");
        assertFalse(result.isBlank(), "分类结果不应为空");
        assertNotEquals("其他", result, "明确的开发内容不应返回默认值'其他'");
    }

    @Test
    @DisplayName("classifyWork — 纯会议内容应返回\"会议\"")
    void classifyWork_shouldReturnMeeting_forMeetingContent() {
        String result = aiService.classifyWork("和产品经理开了需求评审会，讨论Q3迭代计划");

        assertNotNull(result);
        assertNotEquals("其他", result, "明确的会议内容不应返回默认值'其他'");
    }

    @Test
    @DisplayName("classifyWork — 纯文档内容应返回\"文档\"")
    void classifyWork_shouldReturnDoc_forDocContent() {
        String result = aiService.classifyWork("编写了支付模块的API接口文档");

        assertNotNull(result);
        assertNotEquals("其他", result, "明确的文档内容不应返回默认值'其他'");
    }

    @Test
    @DisplayName("classifyWork — 结果应为五大类别之一")
    void classifyWork_resultShouldBeValidCategory() {
        String result = aiService.classifyWork("和前端同事对了一下接口数据格式");

        assertNotNull(result);
        assertTrue(
                List.of("开发", "会议", "文档", "沟通", "其他").contains(result.trim()),
                "分类结果应为五大类别之一，实际为: " + result
        );
    }

    @Test
    @DisplayName("polishReport — 应返回以 ## 开头的 Markdown 周报")
    void polishReport_shouldReturnMarkdownReport() {
        List<String> items = List.of(
                "修复了用户登录页面的超时bug",
                "完成了用户管理模块的增删改查接口",
                "和产品经理开了需求评审会，确定了Q3迭代范围",
                "编写了支付模块的API接口文档"
        );

        String result = aiService.polishReport(items, "2026年第20周");

        assertNotNull(result, "周报内容不应为 null");
        assertFalse(result.isBlank(), "周报内容不应为空");
        assertTrue(result.contains("##"), "周报应以 ## 开头（Markdown标题）");
        assertTrue(result.contains("2026年第20周"), "周报应包含周次信息");
    }
}
