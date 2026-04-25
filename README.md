# 创享引擎 (CreativeEngine)

![Project Status](https://img.shields.io/badge/status-active-success.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)
![Miniprogram](https://img.shields.io/badge/WeChat-Miniprogram-07C160.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## 📖 项目简介

**创享引擎 (Innovation Engine)** 是一个专为“青年创享者”打造的综合性创业赋能平台。项目采用全新的**极简杂志风 (Minimalist Magazine Style)** 设计语言，将美学与实用性完美结合。
平台集成了行业图谱、政策匹配、BP智写、专家对接、AI图像解析等多种核心功能，并接入了 DeepSeek AI 智能助手，旨在为青年创业者提供从想法萌芽到落地执行的全方位、一站式支持。

## 🎯 核心功能模块

### 1. 首页核心引擎 (Innovation Engine)
*   **职业匹配**: 根据专业和技能精准定位发展方向。
*   **政策匹配**: 支持“毕业生 / 创业 / 大学生”等关键词单独检索或组合检索，获取国家扶持政策。
*   **图说解读**: AI 智能读图与图像分析。
*   **资源对接**: 高效寻找优质专家库与场地资源。

### 2. 个人工作台 (Profile)
*   **数据看板**: 直观展示“已读行业”、“适配政策”、“对接资源”等核心足迹。
*   **高频服务**: 快捷入口直达“行业图谱”、“我的匹配”、“BP 智写”、“专家对接”。
*   **管理中心**: 企业档案、资质认证、消息通知等一体化管理。

### 3. AI 智能驱动 (AI Assistant)
*   基于 **DeepSeek AI** 驱动的智能对话与创业咨询解答。
*   BP (商业计划书) 智能辅助撰写。

### 4. 政策数据增强
*   内置 `gov.cn` 政策抓取脚本，低频抓取公开政策页面并生成 MySQL 导入 SQL。
*   政策正文会清洗导航、来源、原文链接等页面杂项，详情页展示更接近阅读型文本。
*   支持多关键词拆分搜索，例如 `大学生创业`、`毕业生+创业`、`毕业生,创业,大学生`。

---

## 🛠️ 技术栈架构

### 后端技术 (Spring Boot)
*   **核心框架**: Spring Boot 3.1.5
*   **编程语言**: Java (JDK 17)
*   **数据库**: MySQL 8.0+
*   **ORM框架**: MyBatis Plus 3.5.5
*   **HTTP通信**: OkHttp 4.11.0 (用于对接外部 AI 接口)
*   **AI底层**: DeepSeek AI 大模型 API
*   **其他组件**: Lombok, Gson, Validation

### 前端技术 (微信小程序)
*   **开发平台**: 微信小程序原生框架 (WXML, WXSS, JS)
*   **设计语言**: 极简杂志风黑白排版 (Minimalist Magazine Style)
*   **UI 布局**: Flexbox 高级流式布局，自适应滚动视图 (Scroll-View) 完美兼容。
*   **AppID**: `wx32588fde0ff73bbb`

---

## 📂 项目结构说明

```text
CreativeEngine/
├── miniprogram/               # 微信小程序前端 (极简杂志风 UI)
│   ├── pages/                 # 小程序页面目录
│   │   ├── index/             # 首页 (Innovation Engine)
│   │   ├── mine/              # 个人中心 (Profile)
│   │   ├── policy/            # 政策列表
│   │   ├── policy-detail/     # 政策详情
│   │   ├── resource/          # 资源对接
│   │   ├── ai-assistant/      # AI 助手
│   │   ├── image-interpretation/ # 图说解读
│   │   ├── career-match/      # 职业匹配
│   │   └── login/             # 登录/注册模块
│   ├── utils/                 # 前端工具类与网络请求封装
│   ├── images/                # 图片/图标资源库
│   └── app.js/json/wxss       # 小程序全局配置
├── src/                       # Spring Boot 后端源码
│   ├── main/
│   │   ├── java/com/mingde/creativeengine/
│   │   │   ├── controller/    # RESTful API 控制器
│   │   │   ├── service/       # 业务逻辑层
│   │   │   ├── mapper/        # MyBatis Mapper 层
│   │   │   ├── entity/        # 数据库实体类
│   │   │   ├── common/        # 全局通用组件
│   │   │   ├── config/        # 配置类 (跨域、拦截器等)
│   │   │   └── exception/     # 全局异常处理
│   │   └── resources/         # 后端资源文件
│   │       └── application.yml# Spring Boot 核心配置
├── scripts/                   # 政策抓取、导入说明与生成 SQL
├── creative_engine.sql        # 数据库建表与初始化脚本
├── pom.xml                    # Maven 依赖配置
└── uploads/                   # 用户上传文件存储目录
```

---

## 🚀 快速开始

### 环境准备
1. **Java 17** 及以上版本。
2. **Maven 3.6+**。
3. **MySQL 8.0+**。
4. **微信开发者工具**。

### 步骤 1：数据库配置
在 MySQL 中创建数据库并导入表结构：
```sql
CREATE DATABASE creative_engine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE creative_engine;
SOURCE /path/to/creative_engine.sql;
```

### 步骤 2：配置本地密钥
不要把真实 DeepSeek API Key 写入 Git。推荐使用环境变量，或在本地创建已被 `.gitignore` 忽略的 `src/main/resources/application-secret.yml`。

环境变量方式：
```bash
DB_PASSWORD=your_mysql_password
DEEPSEEK_API_KEY=your_deepseek_api_key
```

本地私有配置方式：
```yaml
# src/main/resources/application-secret.yml
spring:
  datasource:
    password: your_mysql_password

deepseek:
  api-key: your_deepseek_api_key
```

### 步骤 3：启动后端服务
在根目录下运行：
```bash
mvn clean install
mvn spring-boot:run
```
*(服务默认启动在 `http://localhost:8080`)*

### 步骤 4：导入政策数据
生成并导入“毕业生 / 创业 / 大学生”相关政策：
```bash
node scripts/import-gov-graduate-policies.js "--keyword=毕业生,创业,大学生" --pages=1 --output=scripts/generated/graduate_policies.sql
mysql -uroot -p creative_engine < scripts/generated/graduate_policies.sql
```

### 步骤 5：启动微信小程序
1. 打开 **微信开发者工具**。
2. 导入项目中的 `miniprogram` 文件夹。
3. 如果尚未配置，填写您的 `AppID`，如果只是本地调试，可选择“测试号”。
4. 在详情设置中勾选 **“不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”**。
5. 编译运行即可预览“极简杂志风”精美 UI！

---

## 🌐 核心 API 参考

| 模块 | 接口路径 | 方法 | 说明 |
| --- | --- | --- | --- |
| **用户** | `/api/auth/login` | POST | 用户登录鉴权 |
| **用户** | `/api/auth/register` | POST | 用户注册 |
| **政策** | `/api/policy-data/list` | GET | 获取政策列表，支持 `keyword` 多词检索 |
| **政策** | `/api/policy-data/detail/{id}` | GET | 获取政策详情 |
| **政策** | `/api/policy-data/ask` | POST | 基于政策内容进行 AI 问答 |
| **AI 引擎** | `/api/ai/chat` | POST | 唤起 DeepSeek 智能对话 |
| **职业/行业**| `/api/occupation/match` | POST | 提交信息进行职业匹配 |
| **轮播图** | `/api/banner/list` | GET | 首页动态轮播获取 |

*(更多详细接口说明请在启动项目后访问 `http://localhost:8080/api/doc.html` 查看 Swagger 文档)*

---

## 💡 开发与设计理念

1. **“Less is More” 的视觉体验**: 小程序端抛弃了冗余的卡片与色块，采用黑白高对比的杂志风设计，并引入恰到好处的微交互，让“青年创享者”享受顶级阅读与使用体验。
2. **前后端完全分离**: 标准 RESTful 规范，JSON 数据交互，提升扩展性。
3. **AI 深度赋能**: 不只是简单的工具，而是将 AI 作为“联合创始人”，深度参与到 BP 撰写、行业分析中。

---

## 📄 许可证

本项目基于 **MIT License** 开源。

**感谢使用创享引擎！祝您创业顺利！** 🚀
