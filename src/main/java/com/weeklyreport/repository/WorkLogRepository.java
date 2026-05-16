package com.weeklyreport.repository;

import com.weeklyreport.model.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

    @Query("SELECT w FROM WorkLog w WHERE w.deleted = false AND w.logDate = :date ORDER BY w.createdAt DESC")
    List<WorkLog> findByDate(@Param("date") LocalDate date);

    @Query("SELECT w FROM WorkLog w WHERE w.deleted = false AND w.logDate BETWEEN :start AND :end ORDER BY w.logDate DESC, w.createdAt DESC")
    List<WorkLog> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT w FROM WorkLog w WHERE w.deleted = false ORDER BY w.logDate DESC, w.createdAt DESC")
    List<WorkLog> findAllActive();

    @Modifying
    @Transactional
    @Query("UPDATE WorkLog w SET w.deleted = true WHERE w.deleted = false AND w.logDate = :date")
    int softDeleteByDate(@Param("date") LocalDate date);
}
