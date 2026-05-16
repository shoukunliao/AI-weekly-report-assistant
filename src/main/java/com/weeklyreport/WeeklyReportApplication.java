package com.weeklyreport;

import com.weeklyreport.service.SettingsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WeeklyReportApplication {

    public static void main(String[] args) {
        SettingsService.applyOnStartup();
        SpringApplication.run(WeeklyReportApplication.class, args);
    }
}
