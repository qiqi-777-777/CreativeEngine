# 创享引擎 CreativeEngine

创享引擎是一个面向大学生、青年创业者和早期项目团队的一站式创业赋能平台。项目以前后端分离方式构建：前端采用微信小程序原生框架，后端采用 Spring Boot 3.1.5，围绕职业匹配、政策匹配、AI 创业助手、BP 智写、行业图谱、资源对接、个人工作台等场景，为用户提供从方向判断到材料生成、政策查询和资源沉淀的完整支持。

## 项目亮点

- **极简高端 UI 设计**：全站深度重构，应用高级杂志风格排版（Editorial Magazine Style）与极简黏土风（Claymorphism），搭配流畅的物理微动效，提供兼具美感与现代感的高品质使用体验。
- **微信小程序入口**：覆盖首页、AI 助手、我的、政策匹配、职业匹配、BP 智写、行业图谱、资源对接、专家对接等页面。
- **Spring Boot 后端服务**：提供认证、政策数据、职业匹配、AI 会话、用户个人数据、Banner、BP 导出等 REST API。
- **DeepSeek AI 接入**：支持创业咨询、政策问答、BP 内容生成、职业匹配补充推荐等智能能力。
- **政策数据增强**：内置 `gov.cn` 公开政策抓取脚本，可按关键词生成 MySQL 导入 SQL。
- **用户数据沉淀**：支持政策收藏、职业匹配记录、BP 草稿、通知消息和个人统计。
- **前后端分离**：小程序通过 `http://localhost:8080/api` 访问后端接口，便于后续扩展管理端或 Web 端。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 小程序端 | 微信小程序原生框架、WXML、WXSS、JavaScript、自定义 TabBar |
| 后端 | Java 17、Spring Boot 3.1.5、Spring Web、Spring Validation |
| 数据访问 | MyBatis Plus 3.5.5、MySQL 8.0+ |
| AI/外部调用 | DeepSeek API、OkHttp 4.11.0、Gson |
| 工程工具 | Maven、Lombok、Node.js 脚本 |

## 目录结构

```text
CreativeEngine/
├─ miniprogram/                         # 微信小程序前端
│  ├─ pages/                            # 页面目录
│  │  ├─ index/                         # 首页
│  │  ├─ ai-assistant/                  # AI 助手
│  │  ├─ policy/                        # 政策列表/匹配
│  │  ├─ policy-detail/                 # 政策详情与政策问答
│  │  ├─ career-match/                  # 职业匹配
│  │  ├─ career-match-result/           # 职业匹配结果
│  │  ├─ bp-writer/                     # BP 智写
│  │  ├─ bp-drafts/                     # BP 草稿
│  │  ├─ industry-atlas/                # 行业图谱
│  │  ├─ expert-connect/                # 专家对接
│  │  ├─ resource/                      # 资源对接
│  │  ├─ policy-favorites/              # 政策收藏
│  │  ├─ career-records/                # 职业匹配记录
│  │  ├─ notifications/                 # 通知
│  │  ├─ settings/                      # 设置
│  │  └─ mine/                          # 我的
│  ├─ utils/                            # 请求、接口和本地数据工具
│  ├─ images/                           # 图片与图标资源
│  └─ custom-tab-bar/                   # 自定义底部导航
├─ src/main/java/com/mingde/creativeengine/
│  ├─ controller/                       # REST API 控制器
│  ├─ service/                          # 业务服务
│  ├─ mapper/                           # MyBatis Mapper
│  ├─ entity/                           # 数据实体
│  ├─ config/                           # 跨域、静态资源、MyBatis 配置
│  ├─ common/                           # 统一返回结构
│  └─ exception/                        # 全局异常处理
├─ src/main/resources/
│  ├─ application.yml                   # 后端主配置
│  └─ application.properties
├─ backend/sql/                         # 政策表结构、查询和种子数据
├─ scripts/                             # 政策抓取、导入和数据库扩展脚本
├─ creative_engine.sql                  # 数据库初始化脚本
├─ uploads/                             # 上传文件目录
└─ pom.xml                              # Maven 配置
```

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Node.js 16+（仅政策抓取脚本需要）
- 微信开发者工具

### 2. 初始化数据库

```sql
CREATE DATABASE creative_engine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

然后导入项目根目录的数据库脚本：

```bash
mysql -uroot -p creative_engine < creative_engine.sql
```

如需启用用户个人数据相关表，可同时检查并导入：

```bash
mysql -uroot -p creative_engine < scripts/user_personal_data_schema.sql
```

### 3. 配置后端

后端配置文件位于 `src/main/resources/application.yml`，默认端口为 `8080`，默认数据库为本机 `creative_engine`。

建议使用环境变量保存敏感信息：

```bash
DB_PASSWORD=your_mysql_password
DEEPSEEK_API_KEY=your_deepseek_api_key
```

也可以在本地创建 `src/main/resources/application-secret.yml`，该文件不应提交到 Git：

```yaml
spring:
  datasource:
    password: your_mysql_password

deepseek:
  api-key: your_deepseek_api_key
```

### 4. 启动后端

```bash
mvn spring-boot:run
```

启动后可访问：

```text
http://localhost:8080
http://localhost:8080/health
http://localhost:8080/api/test
```

### 5. 运行小程序

1. 打开微信开发者工具。
2. 导入项目中的 `miniprogram` 目录。
3. 根据需要配置 AppID，或使用测试号本地调试。
4. 本地联调时，在微信开发者工具中勾选“不校验合法域名、web-view、TLS 版本以及 HTTPS 证书”。
5. 确认 `miniprogram/app.js` 中 `globalData.apiBase` 指向：

```js
apiBase: 'http://localhost:8080/api'
```

## 政策数据导入

项目提供了中国政府网公开政策抓取脚本，可按关键词低频抓取政策页面并生成 SQL。

```bash
node scripts/import-gov-graduate-policies.js "--keyword=毕业生 创业,大学生 就业" --pages=1 --output=scripts/generated/graduate_policies.sql
mysql -uroot -p creative_engine < scripts/generated/graduate_policies.sql
```

定时导入可运行：

```bash
node scripts/schedule-gov-policy-import.js
```

更多说明见 `scripts/README-gov-policy-import.md`。

## 核心接口

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 健康检查 | GET | `/health` | 后端服务状态 |
| 认证 | POST | `/api/auth/login` | 用户登录 |
| 认证 | POST | `/api/auth/register` | 用户注册 |
| 认证 | GET/PUT | `/api/auth/userInfo` | 获取或更新用户信息 |
| AI 助手 | POST | `/api/ai/session/create` | 创建 AI 会话 |
| AI 助手 | POST | `/api/ai/chat` | AI 对话 |
| AI 助手 | GET | `/api/ai/history` | 查询会话消息 |
| 政策 | GET | `/api/policy-data/list` | 政策列表，支持 `keyword` |
| 政策 | GET | `/api/policy-data/detail/{id}` | 政策详情 |
| 政策 | POST | `/api/policy-data/ask` | 基于政策内容进行问答 |
| 职业匹配 | GET | `/api/occupation/majors` | 专业选项 |
| 职业匹配 | GET | `/api/occupation/skills` | 技能选项 |
| 职业匹配 | GET | `/api/occupation/personalities` | 性格选项 |
| 职业匹配 | POST | `/api/occupation/match` | 生成匹配结果 |
| 用户数据 | GET | `/api/user-data/stats` | 用户统计 |
| 用户数据 | GET/POST/DELETE | `/api/user-data/bp-drafts` | BP 草稿管理 |
| 用户数据 | GET/POST/DELETE | `/api/user-data/career-records` | 职业记录管理 |
| 用户数据 | GET/POST/DELETE | `/api/user-data/policy-favorites` | 政策收藏管理 |
| 用户数据 | GET/POST/PUT | `/api/user-data/notifications` | 通知管理 |
| BP 导出 | POST | `/api/bp/export/{format}` | 导出 `doc` 或 `pdf` |
| Banner | GET | `/api/banner/list` | 首页轮播图 |
| Banner 管理 | GET/POST/PUT/DELETE | `/api/banner/admin/*` | 轮播图管理 |

## 当前实现状态

- 已接入后端：登录注册、用户信息、AI 助手、政策列表/详情/问答、职业匹配、Banner、用户收藏/记录/草稿/通知、BP 导出。
- 以前端本地数据或原型为主：行业图谱、资源对接、专家对接的部分展示数据。
- 可继续扩展：管理后台、真实专家预约、政策精准画像、微信一键登录、多模态图像理解、BP 文档深度排版导出。

## 常见问题

### 小程序请求失败

检查后端是否启动、`apiBase` 是否为 `http://localhost:8080/api`，并确认微信开发者工具已开启本地调试域名豁免。

### AI 没有回复

检查 `DEEPSEEK_API_KEY` 是否配置，后端控制台是否有外部接口调用错误。

### 数据库连接失败

检查 MySQL 是否启动、数据库名是否为 `creative_engine`，以及 `DB_PASSWORD` 或 `application-secret.yml` 是否正确。

## 许可证

项目当前用于学习、课程设计、原型验证和二次开发。若用于公开发布或商业部署，请先补充正式的 License、隐私政策、数据来源说明和第三方 API 使用条款。
