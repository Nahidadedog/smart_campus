# Smart Campus · 智慧校园服务平台

基于 **Spring Boot 3 + React 19** 的校园信息管理系统，覆盖门户公告/新闻、学生选课与成绩查询、教师成绩录入、领导数据统计、管理员内容维护四类角色。

> 本仓库是重构版本（v2.0）：按行业常见做法重写了安全、异常处理、数据访问与前端架构，详见 [重构说明](#重构说明)。

## 技术栈

| 层 | 技术 | 说明 |
| --- | --- | --- |
| 后端框架 | Spring Boot 3.5 · Java 17 | Web、Validation、Security |
| 认证 | JJWT 0.12 · 无状态 JWT | 密钥与过期时间走配置，不再硬编码 |
| 数据层 | MyBatis-Plus 3.5.18 | `BaseMapper` 通用 CRUD + XML 手写联表 SQL；原子 `UPDATE` 占名额，避免并发超选 |
| 表结构管理 | Flyway 11 | `V1__schema.sql` / `V2__seed.sql` 版本化，H2 与 MySQL 各一份方言脚本 |
| 数据库 | H2（开发/测试）/ MySQL 8（生产） | H2 开启 `MODE=MySQL` 贴近生产语法 |
| 接口文档 | springdoc-openapi 2.x | `/swagger-ui.html` |
| 前端 | React 19 · Vite 8 · React Router 7 | 函数组件 + Hooks |
| UI | Ant Design 6 · @ant-design/icons 6 | `App.useMessage()` 统一提示 |
| HTTP | axios 1.x | 拦截器统一解包 / 401 跳转 |
| 代码质量 | ESLint 10（flat config）· Prettier 3 | `npm run lint` / `npm run format` |

**已移除的依赖**：Spring Data JPA / Hibernate（换成 MyBatis-Plus）、`@types/react*`（JS 项目无需）、无用的 MySQL 驱动残留配置、重复的 `schema.sql`、Lombok 的打包残留。打包后的 jar 从 62.7 MB 降到 40.3 MB。

## 项目结构

```
smart_campus
├── pom.xml / mvnw / .mvn              # Maven Wrapper，无需本地安装 Maven
├── src/main/java/com/huadi/campus
│   ├── common/Result                  # 统一响应体 { code, message, data }
│   ├── config/                        # SecurityConfig、OpenApiConfig、MybatisPlusConfig、AutoFillHandler
│   ├── security/                      # JWT 过滤器、UserDetails、401/403 处理器
│   ├── exception/                     # 业务异常 + 全局异常处理（400/401/403/404/409）
│   ├── enums/                         # 角色、成绩等级、选课状态（带中文标签）
│   ├── entity/                        # MyBatis-Plus 实体（@TableName / @TableId / 自动填充）
│   ├── mapper/                        # BaseMapper 子接口 + @Select/@Update 原子 SQL
│   ├── dto/request · vo               # 入参校验 / 对外视图（不含 password）
│   ├── service/                       # 业务规则
│   └── controller/                    # auth / public / student / teacher / leader / admin
├── src/main/resources
│   ├── application.yml                # 公共配置，默认 dev
│   ├── application-dev.yml            # H2 文件库 + SQL 日志
│   ├── application-prod.yml           # MySQL，全部走环境变量
│   ├── mapper/*.xml                   # 联表查询、动态条件、嵌套 resultMap
│   └── db/migration
│       ├── h2/V1__schema.sql          # H2 方言建表
│       ├── mysql/V1__schema.sql       # MySQL 方言建表
│       └── common/V2__seed.sql        # 演示数据（两边通用，用子查询关联，不写死 id）
└── frontend/
    ├── src/services/api.js            # 所有后端接口集中定义
    ├── src/layouts/RoleLayout.jsx     # 单一布局，按角色渲染菜单
    ├── src/pages/{common,student,teacher,leader,admin}
    └── vite.config.js                 # 开发代理 /api → 8080
```

## 快速开始

环境要求：**JDK 17+**、**Node.js 20+**。无需安装 Maven，直接使用 Wrapper。

### 1. 启动后端（H2，零配置）

```bash
# Windows PowerShell
.\mvnw.cmd spring-boot:run
# macOS / Linux
./mvnw spring-boot:run
```

启动后：

- 首次启动由 Flyway 自动建表并写入演示数据（`data/campus.mv.db`）；想重置演示数据，停掉服务后删除 `data/` 目录即可。
- 后端接口：<http://localhost:8080>
- Swagger UI：<http://localhost:8080/swagger-ui.html>
- H2 控制台：<http://localhost:8080/h2-console>（JDBC URL 填 `jdbc:h2:file:./data/campus;MODE=MySQL`，用户 `sa`，密码空）

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 <http://localhost:3000>，`/api` 由 Vite 代理转发到 8080。

### 3. 演示账号（密码均为 `123456`）

| 角色 | 账号 | 可访问 |
| --- | --- | --- |
| 学生 | `student1` ~ `student4` | 选课、退选、课表、成绩 |
| 教师 | `teacher1` ~ `teacher3` | 我的课程、学生名单、成绩录入 |
| 领导 | `leader1` | 学生/教师名册、统计概览 |
| 管理员 | `admin` | 全部领导功能 + 公告/新闻发布与删除 |

### 4. 运行测试

```bash
.\mvnw.cmd test          # 后端：29 个测试（单元 + MockMvc 集成）
cd frontend && npm run lint && npm run build
```

## API 约定

- 所有响应统一为 `{ "code": 200, "message": "操作成功", "data": ... }`，`code` 与 HTTP 状态码一致。
- 受保护接口需 `Authorization: Bearer <token>`；401 跳登录，403 提示无权限。
- 路径按角色分层：`/api/auth/**`、`/api/public/**`（只读，匿名可访问）、`/api/student/**`、`/api/teacher/**`、`/api/leader/**`、`/api/admin/**`。

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 认证 | POST | `/api/auth/login` | 登录，返回 token + 用户信息 |
| 认证 | POST | `/api/auth/register` | 学生自助注册（忽略传入的角色字段） |
| 门户 | GET | `/api/public/announcements?type=`、`/api/public/news?category=`、`/api/public/courses?semester=&keyword=` | 匿名只读 |
| 学生 | GET/PUT | `/api/student/profile`、`/profile/contact` | 个人信息 / 修改联系方式 |
| 学生 | GET/POST/DELETE | `/api/student/courses[/{id}]` | 我的选课 / 选课 / 退课 |
| 学生 | GET | `/api/student/grades?semester=` | 成绩 |
| 教师 | GET | `/api/teacher/courses`、`/courses/{id}/students`、`/grades` | 授课、名单、成绩 |
| 教师 | PUT | `/api/teacher/courses/{courseId}/grades` | 录入/更新成绩（upsert） |
| 领导 | GET | `/api/leader/statistics/{students\|teachers\|courses}`、`/students`、`/teachers` | 统计与名册 |
| 管理员 | GET/POST/DELETE | `/api/admin/announcements`、`/api/admin/news` | 内容管理 |

完整字段见 Swagger。

## 生产部署

```bash
# 1. 打包
.\mvnw.cmd clean package
cd frontend && npm run build      # 产物在 frontend/dist，交给 Nginx 托管

# 2. 运行（必须注入 JWT_SECRET）
java -jar target/smart-campus-2.0.0.jar --spring.profiles.active=prod
```

环境变量：

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `JWT_SECRET` | JWT 签名密钥，至少 32 字节 | 无（dev 有占位值） |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 地址 | localhost / 3306 / smart_campus |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 账号 | root / 空 |

Nginx 建议配置：`/` 指向 `frontend/dist`，`/api` 反代到 8080，并开启 gzip。

## 重构说明

相对原仓库的主要变化：

**安全**
- 注册接口忽略客户端传入的 `role`，杜绝自助提权为管理员。
- `/api/common/**` 匿名可写的问题修复：只读内容迁到 `/api/public/**`，写操作迁到 `/api/admin/**`。
- 用户实体不再直接序列化（原会泄露 `password` 哈希），统一通过 VO 输出。
- JWT 密钥改配置项 + 环境变量；密钥对象只构建一次；请求头校验通过 `UserDetailsService` 走缓存查找。
- 401 / 403 由 `AuthenticationEntryPoint` / `AccessDeniedHandler` 返回统一 JSON。

**业务**
- 选课名额改为一条 `UPDATE ... WHERE current_students < max_students` 原子占用，解决并发超选。
- 退选后允许再选（复用记录，不触发唯一键冲突）。
- 成绩录入按「学生 + 课程 + 学期」upsert，不再重复插入；校验教师只能给本人课程录分。

**工程**
- 持久层由 Spring Data JPA 换成 MyBatis-Plus 3.5.18：单表 CRUD 走 `BaseMapper`，联表/统计/动态条件写在 XML（`resultMap + association` 映射嵌套对象，`<where>`/`<if>` 拼条件，`GROUP BY` 聚合到 `GroupCount`），`@Update` 原子更新占/退名额，`MetaObjectHandler` 自动填充 `create_time` / `update_time`。
- 表结构与演示数据交给 Flyway 版本化管理（`db/migration/{vendor}` 方言 + `common` 通用），生产 MySQL 启动即自动迁移，不再依赖 `ddl-auto` / `data.sql`。
- 枚举通过 `@JsonValue` 输出中文标签（`已选`、`优`），数据库仍存枚举名；`role` 保持枚举名以便前端按角色鉴权。
- 异常体系改为 `BusinessException` 子类 + `@RestControllerAdvice`，按语义返回 4xx。
- 拆分 `application-{dev,prod}.yml`，测试使用独立的 H2 内存库。
- 前端合并 4 个重复 Layout 为一个配置驱动的 `RoleLayout`；axios 拦截器统一处理响应解包、错误提示、401 跳转；修复课表教师名永远为 undefined、删除按钮无事件等问题。

## 路线图

- [ ] 列表接口分页（MyBatis-Plus `IPage` + `PaginationInnerInterceptor` 已就绪）与 DTO 校验加强
- [ ] 增加 Refresh Token / 多端登录控制
- [ ] 前端引入单元测试（Vitest + Testing Library）
- [ ] CI：GitHub Actions 跑 `mvnw test` 与 `npm run build`

## License

MIT
