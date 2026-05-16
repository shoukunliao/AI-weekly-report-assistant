# CLAUDE.md



This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 要求
- 功能实现规范：每次让你实现功能时，先别急着写代码，先规划该功能框架，使用什么技术实现，操作流程是什么等，如何总结汇报给我，如果有拿不定的，不确定的地方，一定要先问我，再去实现功能。


## Project Overview

AI 周报助手 — a personal productivity tool for daily work logging via chat interface, with AI-powered weekly report generation (DeepSeek). The user chats naturally about what they worked on, and the system records, categorizes, and summarizes it into polished weekly reports.

The `pom.xml` requires `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` — on Windows, omitting it causes Maven to misread Chinese characters as GBK.

## Build / Run Commands

```bash
# Run the application (development)
mvn spring-boot:run

# Build a fat jar
mvn clean package -DskipTests

# Run the jar
java -jar target/ai-weekly-report-assistant-1.0.0.jar

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName
```

The app starts on `http://localhost:8080`. The frontend is served from embedded static resources at `src/main/resources/static/`.

## Environment Variables

- `DEEPSEEK_API_KEY` — DeepSeek API key (required for AI polish/classify features). Defaults to `your-api-key-here` if unset.

## Architecture

### Backend (Spring Boot 3.2.5 + Java 17 + SQLite)

**Controller:**
- `ChatController` — two endpoints: `POST /api/chat` (main interaction) and `GET /api/reminders` (fetch pending reminders). The frontend polls reminders every 30s.

**Service layer (routing cascade):**
1. `ChatService` — entry point for all user messages. Regex-based intent recognition (no LLM): matches slash commands (`/查看`, `/修改`, `/删除`, `/本周周报`, `/上周周报`, `/帮助`) and falls through to work logging for natural language input. Date extraction from Chinese time words (今天/昨天/前天/上周).
2. `WorkLogService` — CRUD with soft-delete. On create and update, calls `AiService.classifyWork()` to auto-categorize.
3. `ReportService` — groups work items by date, builds a raw summary, then calls `AiService.polishReport()` for an AI-enhanced version. Both polished and raw versions are returned.
4. `AiService` — wraps DeepSeek Chat API (`deepseek-chat` model). Two functions: `classifyWork` (returns category label) and `polishReport` (returns structured Markdown report). Both are synchronous — expect up to 15s latency on polish.
5. `ReminderService` — `@Scheduled` job fires at 5pm weekdays (configurable via `reminder.cron`). Queues reminder messages; frontend picks them up via polling.

**Data layer:**
- `WorkLog` entity — fields: id, content, logDate, category, createdAt, deleted (soft-delete).
- `WorkLogRepository` — custom `@Query` methods: `findByDate`, `findByDateRange`, `findAllActive` — all filter out `deleted = true`.
- SQLite via `sqlite-jdbc` driver + Hibernate 6 community dialect (`hibernate-community-dialects` dependency required). DB file: `weekly_report.db` (created automatically in working directory).

**Config:**
- `DeepSeekConfig` — maps `deepseek.*` properties from `application.yml`.
- `AppConfig` — provides `RestTemplate` bean.

### Frontend (Vanilla JS, no build step)

- `index.html` — WeChat-style chat UI with header, message area, input bar, and quick-action buttons.
- `app.js` — POSTs user messages to `/api/chat`, renders Markdown-formatted replies (bold, headings, bullets). Polls `/api/reminders` every 30s. Simple Markdown-to-HTML renderer (no external library).
- `style.css` — WeChat-green theme (`#07c160`), mobile-responsive (max-width 500px).

### Request Flow

```
User types in chat UI
  → POST /api/chat { message: "..." }
  → ChatService.handle() — regex match
    → /command → WorkLogService / ReportService
    → natural language → WorkLogService.add() → AiService.classifyWork() → save to SQLite
  → returns { reply: "..." }
  → frontend renders Markdown in bubble
```

## Key Implementation Notes

- **Intent recognition is keyword-based**, not LLM. The `ChatService.handle()` method is the routing hub — add new commands there.
- **Work classification calls DeepSeek synchronously** on every entry. This is the bottleneck for `/修改` and new work logs. If DeepSeek is slow or down, the category defaults to "其他".
- **Soft delete only** — no hard deletes in the app. The repository layer always filters `deleted = false`.
- **Week boundaries**: Monday–Sunday, computed via `TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)` and `.nextOrSame(DayOfWeek.SUNDAY)`.
- **Frontend Markdown rendering is minimal** — only bold, h2/h3, and bullets are supported. No code blocks, links, or tables.
- **No authentication** — this is a local single-user tool.
  
