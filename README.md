# 创享引擎 (CreativeEngine)

## 项目简介

创享引擎是一个面向青年创业者的综合服务平台，集成了政策查询、AI智能助手、职业匹配、图像解析等多种功能，旨在为创业者提供全方位的支持和服务。

## 技术栈

### 后端技术
- **框架**: Spring Boot 3.1.5
- **Java**: JDK 17
- **数据库**: MySQL 8.0+
- **ORM框架**: MyBatis Plus 3.5.5
- **HTTP客户端**: OkHttp 4.11.0
- **AI服务**: DeepSeek AI
- **工具类**: Lombok, Gson

### 前端技术
- **平台**: 微信小程序
- **AppID**: wx32588fde0ff73bbb

## 项目结构

```
CreativeEngine/
├── backend/                    # 后端备份或额外配置
├── miniprogram/               # 微信小程序前端
│   ├── pages/                # 页面目录
│   │   ├── index/           # 首页
│   │   ├── policy/          # 政策列表
│   │   ├── policy-detail/   # 政策详情
│   │   ├── resource/        # 资源中心
│   │   ├── ai-assistant/    # AI助手
│   │   ├── image-interpretation/ # 图像解析
│   │   ├── career-match/    # 职业匹配
│   │   ├── career-match-result/ # 匹配结果
│   │   ├── mine/            # 我的
│   │   ├── login/           # 登录
│   │   └── register/        # 注册
│   ├── utils/               # 工具类
│   ├── images/              # 图片资源
│   └── app.js/json/wxss     # 小程序配置文件
├── src/                       # 后端源码
│   ├── main/
│   │   ├── java/com/mingde/creativeengine/
│   │   │   ├── controller/  # 控制器层
│   │   │   ├── service/     # 服务层
│   │   │   ├── mapper/      # 数据访问层
│   │   │   ├── entity/      # 实体类
│   │   │   ├── common/      # 公共类
│   │   │   ├── config/      # 配置类
│   │   │   └── exception/   # 异常处理
│   │   └── resources/       # 资源文件
│   │       └── application.yml # 配置文件
│   └── test/                # 测试代码
├── uploads/                   # 文件上传目录
├── creative_engine.sql        # 数据库初始化脚本
└── pom.xml                   # Maven配置文件
```

## 核心功能模块

### 1. 用户系统
- 用户注册/登录
- 用户信息管理
- 隐私政策和用户协议

### 2. 政策服务
- 政策浏览和搜索
- 政策详情查看
- 政策分类筛选

### 3. AI 智能助手
- 基于 DeepSeek AI 的智能对话
- 创业咨询和建议
- 问题解答

### 4. 职业匹配
- 根据专业和技能匹配职业
- 职业信息查询
- 匹配结果展示

### 5. 图像解析
- 图片上传分析
- AI 图像识别
- 智能解读

### 6. 资源中心
- 资源分类浏览
- 资源详情查看

### 7. 轮播管理
- 首页轮播图展示
- 轮播图管理

## 环境要求

### 开发环境
- JDK 17 或更高版本
- Maven 3.6+
- MySQL 8.0+
- 微信开发者工具
- Node.js (可选)

### 推荐 IDE
- IntelliJ IDEA (后端)
- 微信开发者工具 (小程序前端)

## 安装与配置

### 1. 数据库配置

#### 创建数据库
```sql
CREATE DATABASE creative_engine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

#### 导入数据
```bash
mysql -u root -p creative_engine < creative_engine.sql
```

### 2. 后端配置

#### 修改配置文件
编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/creative_engine?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root        # 修改为您的数据库用户名
    password: 123456      # 修改为您的数据库密码

deepseek:
  api-key: sk-xxx       # 修改为您的 DeepSeek API Key
  api-url: https://api.deepseek.com/v1/chat/completions
  model: deepseek-chat
```

#### 安装依赖
```bash
mvn clean install
```

### 3. 小程序配置

#### 修改 AppID
编辑 `project.config.json` 和 `miniprogram/project.config.json`:
```json
{
  "appid": "您的小程序AppID"
}
```

#### 修改 API 地址
编辑小程序中的 API 配置文件（通常在 `utils/config.js` 或 `app.js` 中）:
```javascript
const API_BASE_URL = 'http://localhost:8080/api'; // 修改为您的后端地址
```

## 运行项目

### 1. 启动后端服务

#### 使用 Maven
```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

#### 使用 IDE
在 IntelliJ IDEA 中运行 `CreativeEngineApplication.java`

启动成功后，访问：
- 应用地址: http://localhost:8080
- API文档: http://localhost:8080/api/doc.html (如已配置)

### 2. 启动小程序

1. 打开微信开发者工具
2. 导入项目（选择 `miniprogram` 目录或项目根目录）
3. 填写 AppID
4. 点击编译运行

## API 接口说明

### 基础路径
```
http://localhost:8080/api
```

### 主要接口

#### 用户模块
- `POST /user/login` - 用户登录
- `POST /user/register` - 用户注册
- `GET /user/info` - 获取用户信息

#### 政策模块
- `GET /policy/list` - 获取政策列表
- `GET /policy/detail/{id}` - 获取政策详情
- `POST /policy/search` - 搜索政策

#### AI助手模块
- `POST /ai/chat` - AI对话
- `POST /ai/image-interpretation` - 图像解析

#### 职业匹配模块
- `GET /occupation/list` - 获取职业列表
- `POST /occupation/match` - 职业匹配

#### 轮播图模块
- `GET /banner/list` - 获取轮播图列表

## 常见问题

### 1. 数据库连接失败
- 检查 MySQL 服务是否启动
- 确认数据库用户名和密码是否正确
- 确认数据库 `creative_engine` 是否已创建

### 2. AI 功能无法使用
- 检查 DeepSeek API Key 是否配置正确
- 确认网络连接正常
- 检查 API 额度是否充足

### 3. 小程序无法访问后端
- 确认后端服务已启动
- 检查小程序中的 API 地址配置
- 在微信开发者工具中检查是否开启"不校验合法域名"

### 4. 文件上传失败
- 确认 `uploads` 目录存在且有写权限
- 检查文件大小是否超过限制

## 部署说明

### 后端部署

#### 打包应用
```bash
mvn clean package -DskipTests
```

生成的 JAR 文件位于 `target/CreativeEngine-0.0.1-SNAPSHOT.jar`

#### 运行 JAR
```bash
java -jar target/CreativeEngine-0.0.1-SNAPSHOT.jar
```

#### 使用配置文件
```bash
java -jar target/CreativeEngine-0.0.1-SNAPSHOT.jar --spring.config.location=/path/to/application.yml
```

### 小程序部署

1. 在微信开发者工具中点击"上传"
2. 填写版本号和项目备注
3. 登录微信公众平台
4. 提交审核
5. 等待审核通过后发布

## 开发建议

### 后端开发
- 遵循 RESTful API 设计规范
- 使用统一的返回结果格式
- 添加适当的日志记录
- 编写单元测试

### 前端开发
- 遵循微信小程序开发规范
- 注意小程序性能优化
- 处理好网络异常情况
- 做好用户体验优化

## 安全建议

1. **生产环境**:
   - 修改数据库默认密码
   - 使用环境变量管理敏感信息
   - 启用 HTTPS
   - 配置防火墙规则

2. **API安全**:
   - 实现请求签名验证
   - 添加访问频率限制
   - 做好参数校验和过滤

3. **数据安全**:
   - 定期备份数据库
   - 加密存储敏感信息
   - 使用预编译语句防止SQL注入

## 许可证

本项目采用 MIT 许可证

## 联系方式

如有问题或建议，请联系：
- 项目组: com.mingde
- 邮箱: (请添加您的联系邮箱)

## 更新日志

### v0.0.1-SNAPSHOT (2025-11-26)
- 初始版本发布
- 实现基础功能模块
- 集成 DeepSeek AI 服务

---

**感谢使用创享引擎！祝您创业顺利！** 🚀
