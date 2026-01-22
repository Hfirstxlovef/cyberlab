# 红岸网络空间安全对抗平台 (CyberLab)

> 基于 Spring Boot 和 Vue.js 的网络安全攻防演练实验室系统

[![License](https://img.shields.io/badge/License-NonCommercial-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-brightgreen.svg)](https://vuejs.org/)

## 项目简介

红岸网络空间安全对抗平台是一个专为网络安全教学和攻防演练设计的综合性实训平台，支持红蓝对抗、CTF竞赛、漏洞演练等多种场景。系统采用前后端分离架构，集成 Docker 容器化技术，提供隔离的安全演练环境。

## 核心功能

### 多角色管理体系
- **管理员 (Admin)**: 系统配置、用户管理、全局监控
- **红队 (Red Team)**: 攻击演练、漏洞发现、成果提交
- **蓝队 (Blue Team)**: 防御部署、安全加固、事件响应
- **裁判 (Judge)**: 成果审核、评分管理、竞赛监督

### 容器化演练环境
- Docker 集成管理，支持远程主机部署
- 漏洞容器自动化编排与生命周期管理
- 容器状态实时监控与自动恢复机制
- 支持多种安全场景模板（Web 漏洞、系统渗透、密码破解等）

### 实时对抗监控
- 大屏可视化展示（红队/蓝队/裁判专属视图）
- 攻防态势实时追踪
- 成果流动态更新
- 系统异常告警与处理

### CTF 竞赛系统
- Flag 提交与自动验证
- 成果分级评分（低危/中危/高危/严重）
- 审核工作流与争议处理
- 排行榜与统计分析

### 安全审计
- 容器操作全程审计日志
- 用户行为追踪
- 异常操作检测与告警
- 定时任务与自动化运维

## 技术栈

### 后端 (cyberlab/)
- **核心框架**: Spring Boot 3.5.3 + Java 17
- **数据访问**: Spring Data JPA + Hibernate
- **数据库**: MySQL 8.0+
- **安全认证**: Spring Security + JWT
- **容器管理**: Docker Java API
- **任务调度**: Spring Scheduled Tasks

### 前端 (cyberlab-vue/)
- **框架**: Vue 3 (Composition API)
- **路由**: Vue Router 4
- **UI 组件**: Element Plus
- **HTTP 客户端**: Axios
- **可视化**: ECharts 5
- **构建工具**: Vite 5
- **样式**: Tailwind CSS

### 基础设施
- **容器化**: Docker + Docker Compose
- **Web 服务器**: Nginx (生产环境)
- **开发服务器**: Vite Dev Server (HMR)
- **反向代理**: Vite Proxy (开发环境)

## 快速开始

### 环境要求

- **Java**: JDK 17+
- **Node.js**: 16.x / 18.x / 20.x
- **MySQL**: 8.0+
- **Docker**: 20.10+ (可选，用于容器演练)
- **Maven**: 3.8+ (或使用项目自带的 mvnw)

### 安装步骤

#### 1. 克隆项目

```bash
git clone <repository-url>
cd 红岸网络空间安全对抗平台
```

#### 2. 配置数据库

创建数据库并配置连接信息：

```sql
CREATE DATABASE cyberlab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

编辑 `cyberlab/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cyberlab
    username: your_username
    password: your_password
```

#### 3. 启动后端服务

```bash
cd cyberlab
./mvnw clean package
java -jar target/cyberlab-0.0.1-SNAPSHOT.jar
```

或直接运行：

```bash
./mvnw spring-boot:run
```

后端服务将在 `https://localhost:8443` 启动。

#### 4. 启动前端服务

```bash
cd cyberlab-vue
npm install
npm run dev
```

前端服务将在 `http://localhost:5173` 启动。

#### 5. 默认账户

系统默认创建管理员账户：
- 用户名: `hongan`
- 密码: `hongan123`

**⚠️ 首次登录后请立即修改默认密码！**

### Docker 部署（可选）

使用 Docker Compose 一键部署：

```bash
docker-compose up -d
```

详细的 Docker 部署说明请参阅 [README-DOCKER.md](README-DOCKER.md)。

## 项目结构

```
红岸网络空间安全对抗平台/
├── cyberlab/                      # 后端源码 (Spring Boot)
│   ├── src/main/java/org/cyberlab/
│   │   ├── controller/            # REST API 控制器
│   │   ├── service/               # 业务逻辑层
│   │   ├── entity/                # JPA 实体类
│   │   ├── repository/            # 数据访问层
│   │   ├── security/              # JWT 认证与安全配置
│   │   ├── config/                # 应用配置
│   │   ├── dto/                   # 数据传输对象
│   │   └── task/                  # 定时任务
│   ├── src/main/resources/
│   │   ├── application.yml        # 主配置文件
│   │   └── db/                    # 数据库脚本
│   └── pom.xml                    # Maven 依赖配置
│
├── cyberlab-vue/                  # 前端源码 (Vue 3)
│   ├── src/
│   │   ├── views/                 # 页面组件
│   │   ├── components/            # 通用组件
│   │   ├── api/                   # API 调用封装
│   │   ├── router/                # 路由配置
│   │   ├── utils/                 # 工具函数
│   │   └── composables/           # 组合式函数
│   ├── public/                    # 静态资源
│   ├── server/                    # 录屏服务器
│   └── vite.config.js             # Vite 配置
│
├── docker-compose.yml             # Docker 编排配置
├── CLAUDE.md                      # 项目开发指南
├── README-DOCKER.md               # Docker 部署文档
└── README.md                      # 本文件
```

## 核心模块说明

### 后端核心服务

| 服务类 | 功能描述 |
|--------|---------|
| `DockerService` | Docker 容器生命周期管理 |
| `ContainerStateService` | 容器状态协调与自动恢复 |
| `ContainerPermissionService` | 基于角色的容器访问控制 |
| `ContainerAuditService` | 容器操作审计日志 |
| `ContainerExceptionHandler` | 异常检测与告警 |
| `UserService` | 用户管理与认证 |
| `AchievementService` | 成果提交与审核 |
| `TeamService` | 团队管理 |

### 前端核心页面

| 页面 | 路由 | 角色权限 |
|------|------|---------|
| 管理员仪表盘 | `/admin/dashboard` | Admin |
| 红队大屏 | `/big-screen/red` | Red Team |
| 蓝队大屏 | `/big-screen/blue` | Blue Team |
| 裁判大屏 | `/big-screen/judge` | Judge |
| 用户管理 | `/admin/users` | Admin |
| 容器管理 | `/containers` | Admin, Red, Blue |
| 成果提交 | `/achievements/submit` | Red Team |
| 成果审核 | `/achievements/review` | Judge |

## API 接口

### 认证接口

```http
POST /api/auth/login
POST /api/auth/register
GET  /api/auth/current
```

### 容器管理

```http
GET    /api/drill-containers
POST   /api/drill-containers
PUT    /api/drill-containers/{id}
DELETE /api/drill-containers/{id}
POST   /api/drill-containers/batch-start
POST   /api/drill-containers/batch-stop
```

### 成果管理

```http
GET  /api/achievements
POST /api/achievements
PUT  /api/achievements/{id}/review
```

更多 API 详见后端 Controller 源码。

## 配置说明

### Docker 主机配置

编辑 `cyberlab/src/main/resources/application.yml`：

```yaml
cyberlab:
  docker:
    enabled: true
    host: 172.16.190.130      # Docker 主机 IP
    port: 2375                # Docker API 端口
    remote-enabled: true
    allowed-container-prefixes:
      - cyberlab-
      - drill-
      - vuln-
```

### HTTPS 证书配置

系统默认使用 HTTPS，证书位置：
- 后端: `cyberlab/src/main/resources/keystore.p12`
- 前端: `cyberlab-vue/localhost+2.pem` 和 `localhost+2-key.pem`

### 跨域配置

后端 CORS 配置在 `SecurityConfig.java`，默认允许：
- `https://localhost:5443`
- `https://127.0.0.1:5443`

## 开发指南

### 添加新功能模块

1. **后端**:
   - 创建 Entity (JPA 实体)
   - 创建 Repository (数据访问接口)
   - 创建 Service (业务逻辑)
   - 创建 Controller (REST API)

2. **前端**:
   - 在 `src/api/` 添加 API 调用函数
   - 在 `src/views/` 创建页面组件
   - 在 `src/router/` 配置路由
   - 更新导航菜单

### 代码规范

- 后端遵循阿里巴巴 Java 开发手册
- 前端使用 ESLint + Prettier
- 提交前运行代码检查：
  ```bash
  # 后端
  ./mvnw checkstyle:check
  
  # 前端
  npm run lint
  ```

## 常见问题

### 1. 登录时提示 401 错误

- 检查用户名密码是否正确
- 确认数据库中是否已创建用户
- 查看后端日志排查详细错误

### 2. 容器无法启动

- 确认 Docker 服务是否运行
- 检查 `application.yml` 中 Docker 主机配置
- 验证端口是否被占用

### 3. 前端无法连接后端

- 确认后端服务已启动（端口 8443）
- 检查 CORS 配置是否正确
- 查看浏览器控制台网络请求错误

### 4. 数据库连接失败

- 检查 MySQL 服务状态
- 确认数据库用户名密码正确
- 验证数据库字符集为 utf8mb4

## 性能优化

- JVM 内存配置参见 `.env` 文件
- 数据库连接池使用 HikariCP（最大连接数 5）
- 前端使用 Vite 懒加载优化
- 生产环境建议使用 Nginx 反向代理

## 安全建议

- **生产环境必须修改默认密码**
- 使用强密码策略
- 定期更新依赖版本
- 启用 HTTPS 传输
- 限制 Docker API 暴露范围
- 定期备份数据库

## 贡献指南

本项目仅供学习研究使用，不接受商业用途的贡献。

如需贡献代码：
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 许可证

**本项目采用非商业性使用许可证，仅供学习研究使用。**

版权所有 © 2026 蟑螂恶霸团队

### 许可条款

✅ **允许**:
- 个人学习和研究
- 教学和培训使用
- 非营利性组织使用
- 学术研究引用

❌ **禁止**:
- 商业使用和销售
- 用于盈利性项目
- 未经授权的二次分发
- 移除版权声明

### 免责声明

本软件按"原样"提供，不提供任何明示或暗示的担保，包括但不限于对适销性、特定用途适用性和非侵权性的担保。在任何情况下，作者或版权持有人均不对任何索赔、损害或其他责任负责，无论是在合同诉讼、侵权行为或其他方面。

**使用本系统进行的任何渗透测试、攻击演练等行为，必须在合法授权的范围内进行。严禁用于非法攻击活动，否则后果自负。**

---

## 联系方式

- **团队**: 蟑螂恶霸团队（sun740883686@foxmail.com)
- **项目维护**: 仅限学习交流
- **问题反馈**: 请提交 Issue

## 致谢

感谢所有为网络安全教育事业做出贡献的开发者和研究者。

---

⭐ 如果这个项目对你的学习有帮助，请给我们一个 Star！
