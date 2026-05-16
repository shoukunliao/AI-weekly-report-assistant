package com.weeklyreport.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReminderService {

    private final List<String> pendingReminders = new ArrayList<>();

    @Scheduled(cron = "${reminder.cron}")
    public void sendReminder() {
        String msg = "⏰ 到下班时间啦！今天做了什么？快来记录一下今天的工作内容吧~";
        pendingReminders.add(msg);
    }

    public List<String> fetchPendingReminders() {
        List<String> result = new ArrayList<>(pendingReminders);
        pendingReminders.clear();
        return result;
    }
}
