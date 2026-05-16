package com.weeklyreport.controller;

import com.weeklyreport.service.ChatService;
import com.weeklyreport.service.ReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final ReminderService reminderService;

    public ChatController(ChatService chatService, ReminderService reminderService) {
        this.chatService = chatService;
        this.reminderService = reminderService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        String reply = chatService.handle(message);
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    @GetMapping("/reminders")
    public ResponseEntity<Map<String, Object>> getReminders() {
        List<String> reminders = reminderService.fetchPendingReminders();
        return ResponseEntity.ok(Map.of("reminders", reminders));
    }
}
