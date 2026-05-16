package com.weeklyreport.service;

import com.weeklyreport.model.WorkLog;
import com.weeklyreport.repository.WorkLogRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WorkLogService {

    private final WorkLogRepository repository;
    private final AiService aiService;

    public WorkLogService(WorkLogRepository repository, AiService aiService) {
        this.repository = repository;
        this.aiService = aiService;
    }

    public WorkLog add(String content, LocalDate date) {
        WorkLog log = new WorkLog();
        log.setContent(content);
        log.setLogDate(date);
        log.setCategory(aiService.classifyWork(content));
        return repository.save(log);
    }

    public List<WorkLog> getToday() {
        return repository.findByDate(LocalDate.now());
    }

    public List<WorkLog> getThisWeek() {
        LocalDate start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return repository.findByDateRange(start, end);
    }

    public List<WorkLog> getLastWeek() {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate start = thisMonday.minusWeeks(1);
        LocalDate end = start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return repository.findByDateRange(start, end);
    }

    public List<WorkLog> getAll() {
        return repository.findAllActive();
    }

    public boolean update(Long id, String newContent) {
        Optional<WorkLog> opt = repository.findById(id);
        if (opt.isPresent() && !opt.get().getDeleted()) {
            WorkLog log = opt.get();
            log.setContent(newContent);
            log.setCategory(aiService.classifyWork(newContent));
            repository.save(log);
            return true;
        }
        return false;
    }

    public boolean softDelete(Long id) {
        Optional<WorkLog> opt = repository.findById(id);
        if (opt.isPresent() && !opt.get().getDeleted()) {
            WorkLog log = opt.get();
            log.setDeleted(true);
            repository.save(log);
            return true;
        }
        return false;
    }

    public List<Long> softDeleteBatch(List<Long> ids) {
        List<Long> deleted = new ArrayList<>();
        for (Long id : ids) {
            if (softDelete(id)) {
                deleted.add(id);
            }
        }
        return deleted;
    }

    public List<Long> softDeleteByDate(LocalDate date) {
        List<WorkLog> items = repository.findByDate(date);
        List<Long> deleted = new ArrayList<>();
        for (WorkLog item : items) {
            item.setDeleted(true);
            repository.save(item);
            deleted.add(item.getId());
        }
        return deleted;
    }
}
