<p align="center">
  <h1 align="center">AI 周报助手</h1>
  <p align="center">
    像聊天一样记录日常工作，由 AI 自动分类并生成结构化周报的个人生产力工具。
  </p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-brightgreen" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen" alt="Spring Boot 3.2.5">
  <img src="https://img.shields.io/badge/Database-SQLite-blue" alt="SQLite">
  <img src="https://img.shields.io/badge/AI-DeepSeek-purple" alt="DeepSeek">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License MIT">
</p>

---

## 目录

- [背景](#背景)
- [功能](#功能)
- [快速开始](#快速开始)
- [使用指南](#使用指南)
- [配置](#配置)
- [架构](#架构)
- [路线图](#路线图)
- [贡献](#贡献)
- [许可证](#许可证)

## 背景

每周写周报是开发者的普遍痛点：一周忙碌下来记不清做了什么，手动整理排版费时费力，写出来的内容又不够专业。AI 周报助手通过**每日聊天式碎片记录 + AI 自动汇总润色**的方式，把写周报从负担变成自然而然的事情。

**核心思路：每天花 30 秒随口说几句，周末一条命令出周报。**

## 功能

### 聊天式工作记录

像微信聊天一样输入今天做了什么，系统自动解析时间并进行 AI 分类，无需记忆任何命令。

```
今天上午修复了登录页面的超时 bug
昨天下午参加了需求评审会，确定了 Q2 迭代范围
```

支持的时间词：`今天`、`昨天`、`前天`、`上午`、`下午`、`晚上`

### 命令式数据管理

通过斜杠命令管理所有工作记录，学习成本几乎为零。

| 命令 | 说明 |
|------|------|
| `/查看 今天` | 查看今日工作记录 |
| `/查看 本周` | 查看本周所有记录 |
| `/查看 全部` | 查看全部历史记录 |
| `/修改 1 新内容` | 修改 ID 为 1 的记录 |
| `/删除 1` | 软删除单条记录 |
| `/删除 1,3,5` | 批量删除多条 |
| `/删除 1-5` | 删除指定范围 |
| `/删除 今天` | 删除今日全部 |
| `/本周周报` | 生成本周 AI 润色周报 |
| `/上周周报` | 生成上周周报 |
| `/帮助` | 显示完整帮助信息 |

### AI 驱动

- **自动分类** — 每条记录保存时由 DeepSeek 自动归类（开发 / 会议 / 文档 / 沟通 / 其他）
- **智能润色** — 周报生成时合并相似条目、提炼要点、优化措辞，输出专业 Markdown 周报
- **双重输出** — 同时提供「AI 润色版」和「原始汇总版」，方便对照和自定义修改

### 定时提醒

工作日下午 5 点自动弹出提醒消息，防止遗忘。提醒时间和频率可通过 `reminder.cron` 配置。

### 微信风格 UI

聊天界面模拟微信，消息气泡 + Markdown 渲染，移动端响应式适配。零依赖纯原生实现，无需 Node.js 生态。

## 快速开始

### 环境要求

- **JDK 17** 或更高版本
- **Maven 3.x**

### 启动

```bash
# 1. 进入 Web 项目目录
cd web

# 2. 设置 DeepSeek API Key（可选，不设置则 AI 功能不可用）
# Windows
set DEEPSEEK_API_KEY=sk-your-key-here
# Linux / macOS
export DEEPSEEK_API_KEY=sk-your-key-here

# 3. 启动应用
mvn spring-boot:run
```

浏览器访问 **http://localhost:8080** 即可使用。

### 构建部署

```bash
cd web
mvn clean package -DskipTests
java -jar target/ai-weekly-report-assistant-1.0.0.jar
```

数据库文件 `weekly_report.db` 默认在启动目录生成，可在界面设置中自定义路径。

## 使用指南

### 典型工作流

```
周一上午 9:00   输入："今天上午评审了用户模块的 PRD"
周一下午 2:30   输入："修复了订单列表分页 bug #2341"
周二上午 10:00  输入："昨天下午写完了支付模块的单元测试"
  ...
周五下午 5:30   输入：/本周周报
              → AI 自动生成结构化周报，一键复制到 OA / 飞书
```

### 设置面板

点击聊天界面顶部栏右侧 **"** ⋯ **"** 按钮打开设置：

- **DeepSeek API Key** — 保存后立即生效，优先级高于环境变量
- **数据库文件路径** — 修改后需重启应用

所有设置保存在工作目录下的 `settings.json`。

## 配置

完整配置文件位于 [application.yml](web/src/main/resources/application.yml)：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:sqlite:weekly_report.db

deepseek:
  api-key: ${DEEPSEEK_API_KEY:your-api-key-here}
  model: deepseek-chat

reminder:
  cron: "0 0 17 * * 1-5"   # 周一至周五 17:00
```

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | `8080` |
| `spring.datasource.url` | 数据库路径 | `jdbc:sqlite:weekly_report.db` |
| `deepseek.api-key` | API 密钥 | 环境变量 `DEEPSEEK_API_KEY` |
| `deepseek.model` | 模型名称 | `deepseek-chat` |
| `reminder.cron` | 提醒 Cron 表达式 | `0 0 17 * * 1-5` |

## 架构

### 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Data JPA | 3.2.5 | 数据访问层 |
| Hibernate | 6.4+ | ORM（社区方言支持 SQLite） |
| SQLite | 3.x | 本地零配置数据库 |
| DeepSeek Chat API | — | AI 分类与周报润色 |
| Vanilla JS | — | 前端（无构建步骤） |

### 项目结构

```
├── web/                        # Spring Boot Web 应用
│   └── src/main/java/com/weeklyreport/
│       ├── controller/         # ChatController, SettingsController
│       ├── service/            # ChatService, WorkLogService, ReportService,
│       │                         AiService, ReminderService, SettingsService
│       ├── model/              # WorkLog 实体
│       ├── repository/         # WorkLogRepository (Spring Data JPA)
│       └── config/             # DeepSeekConfig, AppConfig
├── app/                        # 移动端 / 桌面端（规划中）
├── CLAUDE.md                   # Claude Code 配置
└── SPEC.md                     # 产品规格说明书
```

### 请求流程

```
用户输入 → POST /api/chat
  → ChatService.handle() → 正则匹配意图
    ├── /命令  → WorkLogService / ReportService
    └── 自然语言 → WorkLogService.add() → AiService.classifyWork() → SQLite
  → 返回 Markdown 回复
  → 前端渲染消息气泡
```

### 设计决策

- **意图识别基于正则表达式**，不依赖 LLM，保证命令响应 < 500ms
- **软删除**，数据不会物理清除
- **周一至周日**为一周周期
- **本地单用户**，无认证机制
- **AI 同步调用**，DeepSeek 不可用时分类默认降级为"其他"

## 路线图

| 版本 | 内容 |
|------|------|
| v1.0 (已完成) | 工作记录、命令管理、AI 周报生成、AI 润色、定时提醒、聊天 UI |
| v1.1 | 分类优化、周报模板自定义、Markdown 导出 |
| v2.0 | Git 提交记录自动汇总、周报分享链接 |
| v2.1 | 数据看板（工作分布饼图、趋势折线图） |
| v3.0 | 微信 / 飞书机器人接入、多格式导出（PDF / Word） |

## 贡献

本项目为个人生产力工具，欢迎提 Issue 和 PR。

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

MIT License. 详见 [LICENSE](LICENSE) 文件。

---

<p align="center">
  <sub>Made with ❤️ for developers who hate writing weekly reports.</sub>
</p>
