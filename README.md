# AI 周报助手

像聊天一样记录日常工作，由 AI 自动分类并生成结构化周报的个人生产力工具。

## 项目结构

```
├── web/                # Spring Boot Web 应用（当前）
├── app/                # 移动端 / 桌面端（规划中）
├── CLAUDE.md           # Claude Code 配置
└── SPEC.md             # 产品规格说明
```

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Data JPA | 3.2.5 | 数据访问层 |
| Hibernate | 6.4+ | ORM，通过社区方言支持 SQLite |
| SQLite | 3.45.1 | 本地数据库，零配置嵌入式存储 |
| DeepSeek Chat API | — | AI 工作分类与周报润色 |
| Maven | 3.x | 项目构建 |

前端使用原生 HTML/CSS/JavaScript，无构建步骤，无需 Node.js 生态。

## 项目架构

```
web/src/main/java/com/weeklyreport/
├── controller/
│   ├── ChatController.java        # /api/chat、/api/reminders
│   └── SettingsController.java    # /api/settings
├── service/
│   ├── ChatService.java           # 消息路由中心，意图识别
│   ├── WorkLogService.java        # 工作记录 CRUD
│   ├── ReportService.java         # 周报生成
│   ├── AiService.java             # DeepSeek API 调用（分类/润色）
│   ├── ReminderService.java       # 定时提醒（工作日下午5点）
│   └── SettingsService.java       # 设置读写（settings.json）
├── model/
│   └── WorkLog.java               # 工作记录实体
├── repository/
│   └── WorkLogRepository.java     # 数据访问接口
├── config/
│   ├── DeepSeekConfig.java        # DeepSeek 配置属性
│   └── AppConfig.java             # RestTemplate Bean
└── WeeklyReportApplication.java   # 启动入口
```

### 请求流程

```
用户输入 → POST /api/chat
  → ChatService.handle() → 正则匹配意图
    ├── /命令 → WorkLogService / ReportService
    └── 自然语言 → WorkLogService.add() → AiService.classifyWork() → 写入 SQLite
  → 返回 Markdown 格式回复
  → 前端渲染气泡消息
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.x

### 运行

```bash
# 进入 Web 项目目录
cd web

# 设置 DeepSeek API Key（可选，不设置则 AI 功能不可用）
set DEEPSEEK_API_KEY=sk-your-key-here       # Windows
export DEEPSEEK_API_KEY=sk-your-key-here    # Linux/Mac

# 启动
mvn spring-boot:run
```

应用启动后访问 **http://localhost:8080**。

### 打包部署

```bash
cd web
mvn clean package -DskipTests
java -jar target/ai-weekly-report-assistant-1.0.0.jar
```

数据库文件默认在 `web/` 启动目录生成，可通过界面设置自定义路径。

## 功能说明

### 工作记录

像聊天一样输入今天做了什么，系统自动提取日期并调用 AI 分类：

```
今天上午修复了登录页面的超时 bug
昨天下午参加了需求评审会
```

支持的时间词：今天、昨天、前天、上午、下午、晚上

### 命令列表

| 命令 | 说明 |
|------|------|
| `/查看 今天` | 查看今日工作记录 |
| `/查看 本周` | 查看本周所有记录 |
| `/查看 全部` | 查看全部历史记录 |
| `/修改 1 新内容` | 修改 ID 为 1 的记录 |
| `/删除 1` | 删除单条记录 |
| `/删除 1,3,5` | 批量删除多条 |
| `/删除 1-5` | 删除 ID 范围 |
| `/删除 今天` | 删除今日全部记录 |
| `/本周周报` | 生成本周 AI 润色周报 |
| `/上周周报` | 生成上周周报 |
| `/帮助` | 显示帮助信息 |

### AI 自动分类

每条工作记录保存时自动调用 DeepSeek 分类，类别包括：
- 开发、会议、文档、沟通、其他

分类失败时默认归为"其他"。

### 定时提醒

工作日下午 5 点自动弹出提醒消息（可通过 `reminder.cron` 配置调整）。

### 界面设置

点击顶部栏右侧 **⋯** 可打开设置面板：
- **DeepSeek API Key** — 保存后立即生效
- **数据库文件路径** — 修改后需重启应用

设置保存在工作目录下的 `settings.json` 文件中。

## 配置参考

```yaml
# web/src/main/resources/application.yml
server:
  port: 8080                          # 服务端口

spring:
  datasource:
    url: jdbc:sqlite:weekly_report.db # 数据库路径（可通过界面修改）

deepseek:
  api-key: ${DEEPSEEK_API_KEY}        # 优先读环境变量，可在界面覆盖
  model: deepseek-chat

reminder:
  cron: 0 0 17 * * 1-5               # 周一至周五下午5点提醒
```

## 设计说明

- **意图识别**：基于正则表达式，不依赖 LLM，保证命令响应速度
- **软删除**：所有删除操作均为软删除，数据不会物理清除
- **周期计算**：周一至周日为一周，通过 Java Time API 计算
- **无认证**：本工具面向本地单用户使用，未集成登录认证

## 路线图

- [x] Web 端聊天交互与周报生成
- [ ] 移动端 / 桌面端 App 支持
