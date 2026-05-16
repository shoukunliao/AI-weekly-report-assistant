package com.weeklyreport.service;

import com.weeklyreport.model.WorkLog;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final WorkLogService workLogService;
    private final AiService aiService;

    public ReportService(WorkLogService workLogService, AiService aiService) {
        this.workLogService = workLogService;
        this.aiService = aiService;
    }

    public String generateThisWeek() {
        List<WorkLog> items = workLogService.getThisWeek();
        if (items.isEmpty()) {
            return "本周暂无工作记录，现在开始记录吧！";
        }

        LocalDate start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        String range = start.format(DateTimeFormatter.ISO_LOCAL_DATE) + " ~ " + end.format(DateTimeFormatter.ISO_LOCAL_DATE);

        return generateReport(items, range);
    }

    public String generateLastWeek() {
        List<WorkLog> items = workLogService.getLastWeek();
        if (items.isEmpty()) {
            return "上周暂无工作记录。";
        }

        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate start = thisMonday.minusWeeks(1);
        LocalDate end = start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        String range = start.format(DateTimeFormatter.ISO_LOCAL_DATE) + " ~ " + end.format(DateTimeFormatter.ISO_LOCAL_DATE);

        return generateReport(items, range);
    }

    private String generateReport(List<WorkLog> items, String range) {
        StringBuilder raw = new StringBuilder("## " + range + " 周报（原始汇总）\n\n");
        Map<LocalDate, List<WorkLog>> byDate = items.stream()
            .collect(Collectors.groupingBy(WorkLog::getLogDate));

        byDate.forEach((date, logs) -> {
            raw.append("**").append(date.format(DateTimeFormatter.ISO_LOCAL_DATE)).append("**\n");
            for (WorkLog log : logs) {
                String cat = log.getCategory() != null ? "【" + log.getCategory() + "】" : "";
                raw.append("- ").append(cat).append(" ").append(log.getContent()).append("\n");
            }
            raw.append("\n");
        });

        List<String> workItems = items.stream()
            .map(w -> "[" + w.getLogDate() + "] " + w.getContent())
            .collect(Collectors.toList());

        String polished = aiService.polishReport(workItems, range);

        if (polished != null && !polished.isBlank()) {
            return polished + "\n\n---\n" + raw;
        } else {
            return "AI 润色暂时不可用，以下为原始汇总：\n\n" + raw;
        }
    }
}
