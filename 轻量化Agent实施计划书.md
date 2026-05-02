# CreativeEngine 轻量化 Agent 实施计划书

## 一、实施背景

CreativeEngine 当前已经具备微信小程序前端、Spring Boot 后端、MySQL 数据库和 DeepSeek API 接入能力，能够支撑政策查询、职业匹配、AI 助手、BP 智写等创业服务功能。

现阶段项目的 AI 能力主要表现为“单模型多场景调用”，即不同业务页面统一调用 DeepSeek 大模型完成问答、推荐和内容生成。为了进一步提升系统的智能化表达与业务模块边界，可以在现有架构基础上引入轻量化 Agent 设计。

本方案不涉及复杂模型训练、微服务拆分或多模型部署，而是在后端通过“场景角色 Prompt + 业务数据查询 + DeepSeek API 调用”的方式，将不同 AI 能力封装为多个轻量化智能服务模块。

## 二、建设目标

本次轻量化 Agent 改造目标如下：

1. 将现有 AI 能力从通用问答升级为场景化智能服务。
2. 按业务模块封装政策解读、职业规划、BP 写作、创业咨询等 Agent。
3. 保持现有 Spring Boot 单体架构，不引入高复杂度技术栈。
4. 复用现有 MySQL 数据、Service 层和 DeepSeekService。
5. 为后续扩展多 Agent 协同、任务调度和智能推荐闭环预留接口。

## 三、总体设计思路

轻量化 Agent 的核心思想是：

> 不训练模型，不部署多模型，而是基于不同业务场景，为 DeepSeek 大模型设计不同角色、任务边界、输入数据和输出格式。

整体调用流程如下：

```text
用户操作
  ↓
微信小程序页面
  ↓
Spring Boot Controller
  ↓
场景 Agent Service
  ↓
查询业务数据 / 组装专属 Prompt
  ↓
调用 DeepSeek API
  ↓
解析并返回结果
  ↓
小程序页面展示
```

## 四、Agent 模块划分

### 1. 政策解读 Agent

对应业务页面：

- 政策匹配页
- 政策详情页
- 政策问答页

主要职责：

- 根据用户问题查询相关政策数据。
- 对政策条款进行通俗化解释。
- 判断用户或项目是否可能符合政策条件。
- 给出申报建议、注意事项和下一步操作。

输入数据：

- 用户问题
- 政策标题
- 政策正文
- 政策关键词
- 发布时间、适用对象等政策字段

输出结果：

```text
政策解释
适用对象
匹配程度
申报建议
注意事项
```

### 2. 职业规划 Agent

对应业务页面：

- 职业匹配页
- 职业匹配结果页
- 我的匹配记录页

主要职责：

- 根据用户选择的专业、技能、性格特征进行职业推荐。
- 结合数据库中的职业数据计算初步匹配结果。
- 当数据库结果不足时，调用 DeepSeek 生成补充建议。
- 输出职业发展路径和能力提升建议。

输入数据：

- 专业标签
- 技能标签
- 性格标签
- 数据库匹配职业结果

输出结果：

```text
推荐职业
匹配原因
发展方向
能力提升建议
创业方向参考
```

### 3. BP 写作 Agent

对应业务页面：

- BP 智写页
- BP 草稿页
- BP 导出接口

主要职责：

- 根据用户输入的项目信息生成商业计划书内容。
- 按章节生成市场分析、产品介绍、商业模式、团队介绍、融资计划等内容。
- 支持保存草稿并后续继续编辑。

输入数据：

- 项目名称
- 所属行业
- 目标用户
- 用户痛点
- 产品方案
- 团队情况
- 融资需求

输出结果：

```text
项目概述
市场痛点
解决方案
商业模式
团队优势
发展规划
融资计划
```

### 4. 创业咨询 Agent

对应业务页面：

- AI 助手页

主要职责：

- 回答创业流程、团队组建、市场调研、融资、竞品分析等通用问题。
- 根据用户上下文提供创业建议。
- 必要时引导用户进入政策匹配、职业匹配或 BP 智写模块。

输入数据：

- 用户问题
- 当前会话历史
- 可选用户画像信息

输出结果：

```text
问题回答
行动建议
推荐使用的系统功能
后续补充问题
```

### 5. 任务路由 Agent（可选）

对应业务页面：

- AI 助手页
- 后续统一智能入口

主要职责：

- 判断用户输入属于哪类任务。
- 将任务分发给政策解读 Agent、职业规划 Agent、BP 写作 Agent 或创业咨询 Agent。

初期可以不实现该模块，先按页面直接调用对应 Agent。后续如果希望 AI 助手成为统一入口，再加入任务路由 Agent。

输出类型示例：

```text
policy
career
bp
startup
unknown
```

## 五、后端实施方案

### 1. 新增 Agent Service 层

建议在后端新增如下接口或实现类：

```text
src/main/java/com/mingde/creativeengine/service/agent/
├─ PolicyAgentService.java
├─ CareerAgentService.java
├─ BpAgentService.java
├─ StartupAgentService.java
└─ RouterAgentService.java
```

对应实现类：

```text
src/main/java/com/mingde/creativeengine/service/agent/impl/
├─ PolicyAgentServiceImpl.java
├─ CareerAgentServiceImpl.java
├─ BpAgentServiceImpl.java
├─ StartupAgentServiceImpl.java
└─ RouterAgentServiceImpl.java
```

### 2. 复用 DeepSeekService

当前项目已有 `DeepSeekService` 和 `DeepSeekServiceImpl`，后续不需要重复实现 HTTP 调用逻辑。

各 Agent 只负责：

```text
接收业务参数
查询必要数据
拼接专属 Prompt
调用 deepSeekService.chat(prompt)
整理返回结果
```

### 3. 推荐调用关系

```text
PolicyDataController
  ↓
PolicyAgentService
  ↓
PolicyDataService + DeepSeekService

OccupationController
  ↓
CareerAgentService
  ↓
OccupationService + DeepSeekService

AiAssistantController
  ↓
StartupAgentService / RouterAgentService
  ↓
DeepSeekService

BpExportController / BP 相关接口
  ↓
BpAgentService
  ↓
UserDataService + DeepSeekService
```

## 六、接口设计建议

### 1. 政策解读 Agent 接口

```text
POST /api/agent/policy/ask
```

请求参数：

```json
{
  "policyId": 1,
  "question": "大学生创业可以申请哪些补贴？"
}
```

返回结果：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "answer": "根据该政策内容，大学生创业可重点关注创业补贴、场地支持和税费减免等方向……"
  }
}
```

### 2. 职业规划 Agent 接口

```text
POST /api/agent/career/suggest
```

请求参数：

```json
{
  "majors": ["计算机科学与技术"],
  "skills": ["Java", "数据分析"],
  "personalities": ["研究型"]
}
```

返回结果：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "summary": "你的专业和技能更适合技术产品、数据分析和软件开发相关方向。",
    "suggestions": []
  }
}
```

### 3. BP 写作 Agent 接口

```text
POST /api/agent/bp/generate
```

请求参数：

```json
{
  "projectName": "校园二手交易平台",
  "industry": "互联网服务",
  "targetUsers": "高校学生",
  "painPoint": "闲置物品流通效率低",
  "solution": "基于小程序的校园二手交易与信用评价平台"
}
```

返回结果：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "content": "一、项目概述……"
  }
}
```

### 4. 创业咨询 Agent 接口

```text
POST /api/agent/startup/chat
```

请求参数：

```json
{
  "question": "我想做一个校园创业项目，第一步应该做什么？"
}
```

返回结果：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "answer": "建议先从问题验证开始，明确目标用户、痛点和现有替代方案……"
  }
}
```

## 七、Prompt 设计示例

### 1. 政策解读 Agent Prompt

```text
你是一个面向大学生和青年创业者的政策解读助手。
请基于以下政策内容回答用户问题，不要编造政策中不存在的信息。

政策标题：{policyName}
政策关键词：{keywords}
政策内容：{content}

用户问题：{question}

请按照以下结构回答：
1. 政策含义
2. 适用对象
3. 用户可能匹配的条件
4. 申报建议
5. 注意事项
```

### 2. 职业规划 Agent Prompt

```text
你是一个职业规划顾问，服务对象是大学生和青年创业者。
请根据用户的专业、技能和性格特征，给出职业方向建议。

专业：{majors}
技能：{skills}
性格：{personalities}
数据库初步匹配结果：{dbMatches}

请输出：
1. 综合分析
2. 推荐职业方向
3. 匹配原因
4. 能力提升建议
5. 可延伸的创业方向
```

### 3. BP 写作 Agent Prompt

```text
你是一名商业计划书写作助手。
请根据用户提供的项目信息，生成结构清晰、适合大学生创业项目使用的 BP 内容。

项目名称：{projectName}
所属行业：{industry}
目标用户：{targetUsers}
核心痛点：{painPoint}
解决方案：{solution}

请输出以下章节：
1. 项目概述
2. 市场痛点
3. 产品方案
4. 商业模式
5. 竞争优势
6. 发展规划
```

### 4. 创业咨询 Agent Prompt

```text
你是一个创业咨询助手，面向大学生、青年创业者和早期项目团队。
请用清晰、可执行的方式回答用户问题。

用户问题：{question}

回答要求：
1. 先直接回答问题
2. 给出具体行动步骤
3. 如果适合，推荐用户使用政策匹配、职业匹配或 BP 智写功能
4. 不要输出空泛口号
```

## 八、实施阶段安排

### 第一阶段：页面级 Agent 封装

周期建议：1 到 2 天

主要任务：

1. 新建 Agent Service 包。
2. 封装 PolicyAgentService、CareerAgentService、BpAgentService、StartupAgentService。
3. 每个 Agent 使用独立 Prompt。
4. 复用现有 DeepSeekService 调用大模型。
5. 按页面调用对应 Agent，不做自动路由。

阶段成果：

```text
政策页可以调用政策 Agent
职业页可以调用职业 Agent
BP 页可以调用 BP Agent
AI 助手页可以调用创业咨询 Agent
```

### 第二阶段：统一 Agent Controller

周期建议：1 天

主要任务：

1. 新增 AgentController。
2. 暴露 `/api/agent/*` 相关接口。
3. 统一请求参数和返回结构。
4. 与小程序页面完成联调。

阶段成果：

```text
前端可以通过统一 Agent 接口调用不同智能服务
后端 Agent 能力边界更清晰
```

### 第三阶段：任务路由 Agent

周期建议：1 到 2 天

主要任务：

1. 新增 RouterAgentService。
2. 判断用户问题类型。
3. 根据类型调用对应 Agent。
4. 在 AI 助手页实现统一智能入口。

阶段成果：

```text
用户在 AI 助手中提出问题后，系统可自动判断任务类型并分发给对应 Agent
```

### 第四阶段：记录沉淀与体验优化

周期建议：1 到 2 天

主要任务：

1. 将 Agent 生成结果保存到用户记录、BP 草稿或咨询历史。
2. 增加失败兜底回复。
3. 优化 Prompt 输出格式。
4. 补充前端加载态、错误态和结果展示样式。

阶段成果：

```text
Agent 能力与用户数据沉淀打通
形成更完整的智能服务闭环
```

## 九、数据库改造建议

轻量化 Agent 初期可以不新增数据库表，直接复用现有表。

可复用数据包括：

```text
policy_data              政策数据
occupation               职业数据
user                     用户数据
user_bp_draft            BP 草稿
user_career_record       职业匹配记录
user_policy_favorite     政策收藏
user_notification        通知消息
```

如果后续需要记录 Agent 调用历史，可以新增一张表：

```text
agent_session_log
```

建议字段：

```text
id
user_id
agent_type
input_text
output_text
related_id
create_time
```

其中 `agent_type` 可取：

```text
policy
career
bp
startup
router
```

## 十、前端改造建议

前端不需要大规模重构，只需在对应页面接入新的 Agent 接口。

建议改造点：

1. 政策详情页增加“AI 解读该政策”入口。
2. 职业匹配结果页增加“AI 发展建议”模块。
3. BP 智写页调用 BP Agent 生成分章节内容。
4. AI 助手页后续可接入 RouterAgent，实现统一智能问答入口。
5. 我的页面可展示 AI 生成历史、BP 草稿和职业匹配记录。

## 十一、风险与控制

### 1. AI 输出不稳定

控制方式：

- 使用固定 Prompt 模板。
- 限定输出结构。
- 对空结果或异常结果设置兜底文案。

### 2. AI 可能编造政策内容

控制方式：

- 政策 Agent 必须基于数据库政策内容回答。
- Prompt 中明确要求“不得编造政策中不存在的信息”。
- 对涉及申报金额、时间、条件等敏感内容提示用户以官方政策为准。

### 3. 接口响应时间较长

控制方式：

- 前端增加加载状态。
- 后端设置合理超时时间。
- 对非必要 AI 生成内容采用用户主动触发方式。

### 4. 功能边界不清晰

控制方式：

- 每个 Agent 只服务一个主要场景。
- 不同 Agent 使用不同 Prompt 和接口。
- 后续通过 RouterAgent 做统一分发，而不是让单个 Agent 处理所有问题。

## 十二、预期成果

完成轻量化 Agent 改造后，项目将具备以下能力：

1. 政策模块从“政策查询”升级为“政策解读与申报建议”。
2. 职业模块从“结果匹配”升级为“职业规划建议”。
3. BP 模块从“文本填写”升级为“商业计划书智能生成”。
4. AI 助手从“通用问答”升级为“创业咨询智能助手”。
5. 后端 AI 能力从单一 DeepSeek 调用升级为多场景 Agent 服务封装。

## 十三、PPT 表述建议

如果需要写进答辩 PPT，可以使用以下表述：

> 本项目采用轻量化 Agent 设计思路，在不进行复杂模型训练和微服务拆分的前提下，基于 Spring Boot 后端封装多个场景化智能服务模块。系统通过不同角色 Prompt、业务数据查询和 DeepSeek API 调用，实现政策解读 Agent、职业规划 Agent、BP 写作 Agent 和创业咨询 Agent，从而将单一大模型调用升级为多场景智能服务能力。

推荐标题：

```text
轻量化 Agent 架构：从单模型调用到场景化智能服务
```

推荐概括：

```text
不同 Agent = 专属角色 Prompt + 业务数据支撑 + DeepSeek API 调用
```

推荐流程：

```text
用户输入
  ↓
场景识别 / 页面入口
  ↓
调用对应 Agent
  ↓
查询业务数据
  ↓
组装 Prompt 并调用 DeepSeek
  ↓
返回智能结果
```

## 十四、结论

轻量化 Agent 方案适合 CreativeEngine 当前项目阶段。它不需要引入复杂的 AI 工程体系，也不需要训练模型，只需要在现有 Spring Boot 后端基础上进行服务封装和 Prompt 设计，就可以让项目从“接入 AI 接口”提升为“具备多场景智能服务能力”的系统。

该方案技术风险低、实现周期短、展示效果明显，适合用于课程设计、项目答辩、原型升级和后续功能扩展。
