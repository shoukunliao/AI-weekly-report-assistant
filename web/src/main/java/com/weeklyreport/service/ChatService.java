package com.weeklyreport.service;

import com.weeklyreport.model.WorkLog;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

@Service
public class ChatService {

    private final WorkLogService workLogService;
    private final ReportService reportService;

    private static final Pattern DELETE_PATTERN = Pattern.compile("^/删除\\s+(.+)");
    private static final Pattern COMMA_IDS = Pattern.compile("^\\d+(,\\d+)+$");
    private static final Pattern RANGE_IDS = Pattern.compile("^(\\d+)-(\\d+)$");
    private static final Pattern SINGLE_ID = Pattern.compile("^\\d+$");
    private static final Pattern UPDATE_PATTERN = Pattern.compile("^/修改\\s+(\\d+)\\s+(.+)", Pattern.DOTALL);
    private static final Pattern VIEW_PATTERN = Pattern.compile("^/查看\\s*(.*)");

    public ChatService(WorkLogService workLogService, ReportService reportService) {
        this.workLogService = workLogService;
        this.reportService = reportService;
    }

    public String handle(String input) {
        if (input == null || input.isBlank()) {
            return "你好！我是周报助手，可以帮你记录每日工作、生成周报。输入 /帮助 查看更多。";
        }

        String trimmed = input.trim();

        if (trimmed.equals("/本周周报")) {
            return reportService.generateThisWeek();
        }

        if (trimmed.equals("/上周周报")) {
            return reportService.generateLastWeek();
        }

        if (trimmed.equals("/帮助") || trimmed.equals("/help")) {
            return getHelp();
        }

        Matcher deleteMatcher = DELETE_PATTERN.matcher(trimmed);
        if (deleteMatcher.matches()) {
            return handleDelete(deleteMatcher.group(1).trim());
        }

        Matcher updateMatcher = UPDATE_PATTERN.matcher(trimmed);
        if (updateMatcher.matches()) {
            long id = Long.parseLong(updateMatcher.group(1));
            String newContent = updateMatcher.group(2).trim();
            boolean ok = workLogService.update(id, newContent);
            return ok ? "已更新记录 #" + id : "未找到记录 #" + id + "，请检查 ID 是否正确。";
        }

        Matcher viewMatcher = VIEW_PATTERN.matcher(trimmed);
        if (viewMatcher.matches()) {
            return handleView(viewMatcher.group(1).trim());
        }

        return handleWorkLog(trimmed);
    }

    private String handleDelete(String args) {
        // 日期关键词
        if (args.equals("今天") || args.equals("昨天")) {
            LocalDate date = args.equals("今天") ? LocalDate.now() : LocalDate.now().minusDays(1);
            List<Long> deleted = workLogService.softDeleteByDate(date);
            if (deleted.isEmpty()) {
                return args + "没有可删除的记录。";
            }
            return "已删除 " + deleted.size() + " 条" + args + "的记录：" + deleted;
        }

        // 逗号分隔的多个 ID：1,3,5
        if (COMMA_IDS.matcher(args).matches()) {
            String[] parts = args.split(",");
            List<Long> ids = new ArrayList<>();
            for (String p : parts) {
                ids.add(Long.parseLong(p));
            }
            List<Long> deleted = workLogService.softDeleteBatch(ids);
            if (deleted.isEmpty()) {
                return "未找到指定的记录，请检查 ID 是否正确。";
            }
            return "已删除 " + deleted.size() + " 条记录：" + deleted;
        }

        // ID 范围：1-5
        Matcher rangeMatcher = RANGE_IDS.matcher(args);
        if (rangeMatcher.matches()) {
            long from = Long.parseLong(rangeMatcher.group(1));
            long to = Long.parseLong(rangeMatcher.group(2));
            if (from > to) {
                return "ID 范围无效，" + from + " > " + to + "。";
            }
            List<Long> ids = new ArrayList<>();
            for (long i = from; i <= to; i++) {
                ids.add(i);
            }
            List<Long> deleted = workLogService.softDeleteBatch(ids);
            if (deleted.isEmpty()) {
                return "未找到指定范围内的记录，请检查 ID 是否正确。";
            }
            return "已删除 " + deleted.size() + " 条记录：" + deleted;
        }

        // 单个 ID
        if (SINGLE_ID.matcher(args).matches()) {
            long id = Long.parseLong(args);
            boolean ok = workLogService.softDelete(id);
            return ok ? "已删除记录 #" + id : "未找到记录 #" + id + "，请检查 ID 是否正确。";
        }

        return "无法识别删除参数「" + args + "」，支持：/删除 5 | /删除 1,3,5 | /删除 1-5 | /删除 今天 | /删除 昨天";
    }

    private String handleView(String filter) {
        List<WorkLog> items;
        String title;

        switch (filter) {
            case "今天":
                items = workLogService.getToday();
                title = "今日工作记录";
                break;
            case "本周":
                items = workLogService.getThisWeek();
                title = "本周工作记录";
                break;
            case "全部":
                items = workLogService.getAll();
                title = "全部工作记录";
                break;
            default:
                items = workLogService.getToday();
                title = "今日工作记录（输入 /查看 本周 或 /查看 全部 查看更多）";
        }

        if (items.isEmpty()) {
            return "暂无" + title + "。";
        }

        StringBuilder sb = new StringBuilder("📋 **").append(title).append("**\n\n");
        for (WorkLog item : items) {
            sb.append("#").append(item.getId())
              .append(" [").append(item.getLogDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
              .append("] ").append(item.getCategory() != null ? "【" + item.getCategory() + "】" : "")
              .append(" ").append(item.getContent()).append("\n");
        }
        return sb.toString();
    }

    private String handleWorkLog(String input) {
        LocalDate date = extractDate(input);
        String content = cleanContent(input);

        if (content.isBlank()) {
            return "没有识别到工作内容，请像这样告诉我：「今天修复了用户登录的超时问题」。";
        }

        WorkLog saved = workLogService.add(content, date);
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String cat = saved.getCategory() != null ? "（分类：" + saved.getCategory() + "）" : "";
        return "✅ 已记录 #" + saved.getId() + " [" + dateStr + "] " + cat + "\n" + saved.getContent();
    }

    private LocalDate extractDate(String input) {
        if (input.contains("昨天")) return LocalDate.now().minusDays(1);
        if (input.contains("前天")) return LocalDate.now().minusDays(2);
        if (input.contains("上周")) return LocalDate.now().minusWeeks(1);
        return LocalDate.now();
    }

    private String cleanContent(String input) {
        return input
            .replaceAll("^(今天|昨天|前天|上周一|上周二|上周三|上周四|上周五|上周六|上周日)\\s*", "")
            .replaceAll("^(上午|下午|晚上|早上|中午)\\s*", "")
            .trim();
    }

    private String getHelp() {
        return """
            📋 **周报助手 - 帮助**

            **记录工作**
            直接描述你今天做了什么，例如：
            - "今天上午修了登录页的 bug"
            - "下午开了需求评审会，确定了Q2迭代范围"
            - "昨天写了支付模块的单元测试"

            **查看记录**
            - `/查看 今天` — 查看今日记录
            - `/查看 本周` — 查看本周记录
            - `/查看 全部` — 查看所有记录

            **修改/删除**
            - `/修改 1 新内容` — 修改记录 #1
            - `/删除 1` — 删除单条记录
            - `/删除 1,3,5` — 批量删除多条
            - `/删除 1-5` — 删除 ID 范围
            - `/删除 今天` — 删除今日全部记录

            **生成周报**
            - `/本周周报` — 生成本周周报（AI润色版）
            - `/上周周报` — 生成上周周报
            """;
    }
}
