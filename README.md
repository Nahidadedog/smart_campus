# Smart Campus · 智慧校园服务平台

基于 **Spring Boot 3 + React 19** 的校园信息管理系统，覆盖门户公告/新闻、学生选课与成绩查询、教师成绩录入、领导数据统计、管理员内容维护四类角色。

选课链路按高并发场景设计：**Redis Lua 预扣名额 + RabbitMQ 异步落库 + 令牌桶限流 + Prometheus 指标**，纯数据库实现作为可切换的基线与兜底，详见 [高并发选课设计](#高并发选课设计)。

> 本仓库是重构版本（v2.0）：按行业常见做法重写了安全、异常处理、数据访问与前端架构，详见 [重构说明](#重构说明)。

## 技术栈

| 层 | 技术 | 说明 |
| --- | --- | --- |
| 后端框架 | Spring Boot 3.5 · Java 17 | Web、Validation、Security、AOP |
| 认证 | JJWT 0.12 · 无状态 JWT | 密钥与过期时间走配置，不再硬编码 |
| 数据层 | MyBatis-Plus 3.5.18 | `BaseMapper` 通用 CRUD + XML 手写联表 SQL |
| 表结构管理 | Flyway 11 | `db/migration/{h2,mysql}` 方言脚本 + `common` 通用脚本 |
| 数据库 | H2（开发/测试）/ MySQL 8（生产） | H2 开启 `MODE=MySQL` 贴近生产语法 |
| 名额预占 | Redis 7 + Lua 脚本 | 判断已选 → 判断余量 → 扣减 → 记录学生，单线程原子执行 |
| 异步落库 | RabbitMQ 3.13 | 生产者确认 + 死信队列，落库失败自动回补名额 |
| 限流 | Redis 令牌桶（Lua） | `@RateLimit` 注解 + SpEL，按用户隔离，fail-open |
| 本地缓存 | Caffeine `AsyncCache` | 课程快照 / userId→studentId；查库在独立线程池执行，桶锁内不做 I/O |
| 可观测 | Actuator + Micrometer + Prometheus + Grafana | `/actuator/prometheus` |
| 接口文档 | springdoc-openapi 2.x | `/swagger-ui.html` |
| 前端 | React 19 · Vite 8 · React Router 7 | 函数组件 + Hooks |
| UI | Ant Design 6 · @ant-design/icons 6 | `App.useMessage()` 统一提示 |
| HTTP | axios 1.x | 拦截器统一解包 / 401 跳转 |
| 代码质量 | ESLint 10（flat config）· Prettier 3 | `npm run lint` / `npm run format` |
| 测试 | JUnit 5 · MockMvc · Testcontainers（Redis / RabbitMQ / MySQL）· Awaitility | 42 个测试，真实中间件容器验证 |
| 压测 | k6 | `loadtest/select-course.js` |

**已移除的依赖**：Spring Data JPA / Hibernate（换成 MyBatis-Plus）、`@types/react*`（JS 项目无需）、无用的 MySQL 驱动残留配置、重复的 `schema.sql`、Lombok 的打包残留。

## 项目结构

```
smart_campus
├── pom.xml / mvnw / .mvn              # Maven Wrapper，无需本地安装 Maven
├── Dockerfile / docker-compose.yml    # 后端镜像；MySQL + Redis + RabbitMQ + 前端 + 监控 + k6
├── src/main/java/com/huadi/campus
│   ├── common/Result                  # 统一响应体 { code, message, data }
│   ├── config/                        # SecurityConfig、OpenApiConfig、MybatisPlusConfig、SelectionConfig
│   ├── security/                      # JWT 过滤器、UserDetails、401/403 处理器
│   ├── exception/                     # 业务异常 + 全局异常处理（400/401/403/404/409/429）
│   ├── enums/                         # 角色、成绩等级、选课状态（带中文标签）
│   ├── entity/                        # MyBatis-Plus 实体（@TableName / @TableId / 自动填充）
│   ├── mapper/                        # BaseMapper 子接口 + @Select/@Update 原子 SQL
│   ├── selection/                     # 高并发选课核心
│   │   ├── StockService (接口) / DbStockService / RedisStockService   # 名额预占两种实现
│   │   ├── SelectionPersistService / Handler                           # 落库 + 幂等 + 回补
│   │   ├── SelectionPersistPublisher: Sync / InProcess / Rabbit       # 三种落库通道
│   │   ├── SelectionService           # 编排：缓存校验 → 预占 → 投递 → 响应
│   │   └── SelectionLocalCache / CourseStockView / SelectionMetrics    # 本地缓存 / 指标
│   ├── ratelimit/                     # @RateLimit 注解 + AOP + Redis 令牌桶
│   ├── dto/request · vo               # 入参校验 / 对外视图（不含 password）
│   ├── service/                       # 业务规则
│   └── controller/                    # auth / public / student / teacher / leader / admin
├── src/main/resources
│   ├── application.yml                # 公共配置，默认 dev（db + sync）
│   ├── application-{dev,prod,docker}.yml
│   ├── lua/*.lua                      # 名额预扣 / 释放 / 重建、令牌桶
│   ├── mapper/*.xml                   # 联表查询、动态条件、嵌套 resultMap
│   └── db/migration
│       ├── h2/V1__schema.sql · h2/V3__course_enrollment_window.sql
│       ├── mysql/V1__schema.sql · mysql/V3__course_enrollment_window.sql
│       └── common/V2__seed.sql        # 演示数据（两边通用，用子查询关联，不写死 id）
├── loadtest/select-course.js          # k6 抢课压测脚本
├── docker/                            # Prometheus / Grafana 配置、Maven 镜像配置
└── frontend/
    ├── src/services/api.js            # 所有后端接口集中定义
    ├── src/layouts/RoleLayout.jsx     # 单一布局，按角色渲染菜单
    ├── src/pages/{common,student,teacher,leader,admin}
    ├── Dockerfile / nginx.conf        # 生产：Nginx 托管静态文件并反代 /api
    └── vite.config.js                 # 开发代理 /api → 8080
```

## 快速开始

环境要求：**JDK 17+**、**Node.js 20+**。无需安装 Maven，直接使用 Wrapper。

### 1. 本地零配置运行（H2 + 纯数据库）

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

默认 `STOCK_MODE=db`、`PERSIST_MODE=sync`，不需要任何中间件。本机有 Redis / RabbitMQ 时可以用环境变量切换：

```bash
$env:STOCK_MODE="redis"; $env:PERSIST_MODE="rabbit"; $env:RATE_LIMIT_ENABLED="true"; .\mvnw.cmd spring-boot:run
```

### 2. Docker 一键全栈（MySQL + Redis + RabbitMQ + 监控）

```bash
docker compose up -d --build
```

- 前端 <http://localhost:3000>，后端 <http://localhost:8080>，Swagger <http://localhost:8080/swagger-ui.html>
- RabbitMQ 管理界面 <http://localhost:15672>（guest / guest），可以看到 `campus.selection.queue` 与死信队列
- Prometheus <http://localhost:9090>，Grafana <http://localhost:3001>（admin / admin，数据源已自动配置）

后端容器以 `prod,docker` 两个 profile 运行：`prod` 负责 MySQL 数据源环境变量，`docker` 负责 Redis / RabbitMQ 地址并开启 `redis + rabbit` 模式与限流。

想跑**纯数据库基线**做对比，改环境变量重建后端即可（MySQL / Redis / RabbitMQ 容器不动）：

```bash
# PowerShell
$env:STOCK_MODE="db"; $env:PERSIST_MODE="sync"; $env:RATE_LIMIT_ENABLED="false"; docker compose up -d --build backend
# Linux / macOS
STOCK_MODE=db PERSIST_MODE=sync RATE_LIMIT_ENABLED=false docker compose up -d --build backend
# 改回高并发版本
docker compose up -d --build backend
```

### 3. 启动前端（本地开发）

```bash
cd frontend
npm install
npm run dev
```

访问 <http://localhost:3000>，`/api` 由 Vite 代理转发到 8080。

### 4. 演示账号（密码均为 `123456`）

| 角色 | 账号 | 可访问 |
| --- | --- | --- |
| 学生 | `student1` ~ `student4` | 选课、退选、课表、成绩 |
| 教师 | `teacher1` ~ `teacher3` | 我的课程、学生名单、成绩录入 |
| 领导 | `leader1` | 学生/教师名册、统计概览 |
| 管理员 | `admin` | 全部领导功能 + 公告/新闻发布 + 课程维护 + 缓存刷新 |

### 5. 运行测试

```bash
.\mvnw.cmd test          # 后端：42 个测试
cd frontend && npm run lint && npm run build
```

| 测试类 | 数量 | 说明 |
| --- | --- | --- |
| `ApiIntegrationTests` / `GradeLevelTests` / `JwtServiceTests` / `AsyncSelectionTests` | 32 | H2 内存库，覆盖登录、各角色接口、选课名额与时间窗口、成绩等级、JWT、进程内异步落库 |
| `RedisRabbitSelectionTests` | 7 | Testcontainers 启动真实 Redis + RabbitMQ：200 并发抢 20 个名额零超卖、重复/释放幂等、懒加载、MQ 异步落库、落库失败回补 + 死信、令牌桶限流 429 |
| `MySqlSelectionTests` | 3 | Testcontainers 启动真实 MySQL 8：mysql 方言迁移脚本、同步落库 / 退选、选课时间窗口与管理员改课 |

两个容器测试类**没有 Docker 时自动跳过**。国内网络拉不到 Docker Hub 镜像时，先通过镜像站拉取并打标签：

```bash
docker pull docker.m.daocloud.io/testcontainers/ryuk:0.12.0 && docker tag docker.m.daocloud.io/testcontainers/ryuk:0.12.0 testcontainers/ryuk:0.12.0
docker pull docker.m.daocloud.io/library/redis:7-alpine && docker tag docker.m.daocloud.io/library/redis:7-alpine redis:7-alpine
docker pull docker.m.daocloud.io/library/rabbitmq:3.13-management-alpine && docker tag docker.m.daocloud.io/library/rabbitmq:3.13-management-alpine rabbitmq:3.13-management-alpine
docker pull docker.m.daocloud.io/library/mysql:8.0 && docker tag docker.m.daocloud.io/library/mysql:8.0 mysql:8.0
```

> 为什么要单独跑 MySQL：H2 不校验只读事务、也不校验 MySQL 特有行为。项目里曾有一个 bug——`StudentService` 类级 `@Transactional(readOnly = true)` 让同步落库加入了外层只读事务，H2 测试全绿，MySQL 直接报 `Connection is read-only`——就是靠这个测试类兜住的。不过连接池层面的死锁（见[压测踩坑](#压测踩坑基线第一次压测时全线超时)）连 Testcontainers 也测不出来：H2 / 低并发下根本不会触发，只有压测能暴露。

## 高并发选课设计

### 请求链路

```
POST /api/student/courses/{id}
  │
  ├─ ① @RateLimit 令牌桶（Redis，按用户）          超限 → 429
  ├─ ② 本地缓存（Caffeine AsyncCache）：学生 / 课程存在、选课时间窗口   不通过 → 404 / 409   ← 未命中时查库在独立线程池
  ├─ ③ StockService.reserve 名额预占
  │       ├─ db 模式：  UPDATE course SET current_students = current_students + 1
  │       │             WHERE id = ? AND current_students < max_students   （行锁保证原子）
  │       └─ redis 模式：Lua：SISMEMBER → GET 余量 → DECR → SADD           （单线程原子）
  │                    Redis 不可用 → 自动降级为 db 模式
  ├─ ④ SelectionPersistPublisher 投递落库指令
  │       ├─ sync：       请求线程直接写库
  │       ├─ in-process： 进程内线程池异步写
  │       └─ rabbit：     RabbitMQ → 消费者写库（生产者确认 + 死信队列）
  │                    投递失败 → 请求线程同步写库兜底
  └─ ⑤ 响应 { status: SUCCESS | PENDING, remaining }
```

热路径上只有一次 Lua 和一次 MQ 投递，数据库写入被挪到消费者里按自己的节奏执行，这就是"削峰"。

### 可切换的模式

| 配置 | 取值 | 用途 |
| --- | --- | --- |
| `STOCK_MODE` | `db` / `redis` | 名额在哪里预占；`redis` 是性能版，`db` 是正确性基线与降级目标 |
| `PERSIST_MODE` | `sync` / `in-process` / `rabbit` | 记录如何落库；`in-process` 用于无 MQ 时演示异步，`rabbit` 是生产形态 |
| `RATE_LIMIT_ENABLED` | `true` / `false` | 限流开关（依赖 Redis） |

三种落库模式共用同一个 `SelectionPersistHandler`，失败语义完全一致；对比压测时只改环境变量即可。

### 一致性与失败处理

| 场景 | 处理 |
| --- | --- |
| 同一学生重复点击 | Lua 里 `SISMEMBER` 命中 → 409「已选择该课程」；上一次请求仍在落库中也会被拦住 |
| 并发抢最后一个名额 | Lua 单线程执行，`DECR` 之前判断余量，不会超卖；集成测试 200 并发抢 20 个名额验证 |
| 两个请求同时落库 | `student_id + course_id` 唯一约束兜底，后者捕获 `DuplicateKeyException`，归还预留名额 |
| Redis 连接失败 / 超时 | `StockUnavailableException` → 降级为数据库原子 UPDATE，指标 `campus.selection.degraded` +1 |
| 缓存缺失（首次访问 / Redis 重启） | Lua 返回 -2 → `SET NX` 互斥下从数据库重建缓存 → 重试一次 |
| Broker 不可达 | 投递抛 `PublishUnavailableException` → 请求线程同步写库，用户无感 |
| 消息发出但未被确认 | 生产者确认回调 nack → 回补名额，避免"扣了名额没记录" |
| 消费者落库失败（如数据库宕机） | 先回补名额，再抛出 → `default-requeue-rejected=false` → 死信队列，不无限重投 |
| 退选 | 数据库事务提交后再释放 Redis 名额（`TransactionSynchronization.afterCommit`），避免回滚后名额多出来 |
| 缓存与数据库漂移 | `POST /api/admin/cache/courses/refresh` 以数据库为准重建；应在低峰、MQ 无积压时执行 |

**一致性模型**：Redis 名额是强一致的（单线程脚本），`course.current_students` 与选课记录是最终一致的（由消费者在同一个事务里写入）。接口在异步模式下返回 `PENDING`，前端稍后刷新"我的选课"。

### 限流

`@RateLimit(key = "'select:' + #principal.id()")`：每个用户一个令牌桶（默认容量 5、每秒补 1），SpEL 从方法参数解析 key，Lua 原子完成"补充 → 判断 → 扣减"。Redis 故障时 **fail-open**——限流只防刷，不承担正确性，名额超卖由预占层兜底。

### 可观测

`/actuator/prometheus` 暴露：

| 指标 | 含义 |
| --- | --- |
| `campus_selection_requests_total{outcome}` | 请求结果：acquired / duplicate / sold_out / not_found / not_open |
| `campus_selection_degraded_total` | Redis 不可用降级到数据库的次数 |
| `campus_selection_publish_failed_total` | 投递失败改为同步落库的次数 |
| `campus_selection_persist_failed_total` | 落库失败并回补名额的次数 |
| `campus_selection_persist_seconds_*` | 落库耗时分布 |
| `http_server_requests_seconds_*` | 接口 P50 / P95 / P99 |

Grafana 里导入 Spring Boot 3.x dashboard（ID 19004）即可直接看。

### 压测

```bash
# 对 docker 编排里的后端（默认 redis + rabbit + 限流）
docker compose --profile loadtest run --rm loadtest
# 自定义规模
STUDENTS=1000 SEATS=100 docker compose --profile loadtest run --rm loadtest
# 对照组：先按上一节把后端切成 db + sync，再跑一次；两边只改环境变量
docker compose --profile loadtest run --rm loadtest
# 对本机 db + sync 模式直接压（不依赖 docker）
k6 run -e BASE_URL=http://localhost:8080 -e STUDENTS=300 -e SEATS=50 loadtest/select-course.js
```

脚本在 `setup` 阶段用 `admin` 创建一门只有 `SEATS` 个名额的课并注册 `STUDENTS` 个学生，`default` 阶段所有人同时抢一次（每人只发一个请求），`teardown` 阶段从数据库读出已选人数，核对 ≤ 名额（零超卖）且 = 名额（全部落库）。名额有限，大多数请求被 409 拒绝是预期行为，只有 5xx / 网络错误才算失败（单独计入 `selection_error`）。

#### 压测数据

环境：Windows 笔记本上的 Docker Desktop（28 核 / 7.6 GB），MySQL 8.0、Redis、RabbitMQ、后端、k6 全部是同一台机器上的容器，**绝对值没有参考意义，只看两种模式的相对关系**。规模：300 个学生同时抢一门 50 个名额的课，每人只发一次请求。每种模式跑 3 次。

| 模式 | 抢到 / 售罄 | 落库 / 零超卖 | 选课接口 p(95)（三次） | 选课接口 avg（热身后） | 300 个请求总耗时 | 失败 |
| --- | --- | --- | --- | --- | --- | --- |
| `db + sync`（纯数据库基线，无限流） | 50 / 250 | 50 / 50 ✓ | 464 / 411 / 420 ms | 364–387 ms | 0.5–0.6 s | 0 |
| `redis + rabbit` + 限流 | 50 / 250 | 50 / 50 ✓ | 344 / 142 / 108 ms | 76–98 ms | 0.2–0.4 s | 0 |

高并发版第一次是容器刚启动的冷启动值（JIT 未热），看后两次。两种模式下 Hikari 泄漏检测、`Connection is not available`、死信队列均为 0。

怎么读这组数据：

- 300 人规模下两种模式都做到了零超卖、零失败——**正确性不是高并发版的专利**，基线靠的是 `UPDATE ... WHERE current_students < max_students` 的行锁。
- 高并发版把名额判断挪到 Redis 单线程 Lua（微秒级），数据库写入挪到 MQ 消费者，请求线程在响应前只做"一次 Lua + 一次投递"，所以延迟低、抖动小。基线的上限由 10 个连接和课程行锁的串行排队决定：选同一门课的 UPDATE 必须一个一个过，人数再往上（例如 3000 人）差距会继续拉开。
- 基线的 avg 和 p(95) 贴得很近（364 vs 411 ms）：几乎所有请求都在连接池 / 行锁上排队，是整体性排队而不是队尾抖动；高并发版 p(95) 约为 avg 的 1.5 倍，绝对值则小得多。

#### 压测踩坑：基线第一次压测时全线超时

基线模式第一次压测的结果是：300 个请求里 299 个 60 s 超时，Hikari 报 `Connection is not available (total=10, active=10, waiting=30)`，但 InnoDB 里一个事务、一个锁等待都没有。这个 bug 在 H2 单测、Docker 冒烟、Redis 模式压测里都没出现过，只在"真实 MySQL + 300 并发 + 纯数据库模式"下必现。

排查过程：

1. `jstack` 看线程：一半线程卡在 `HikariPool.getConnection`，另一半卡在 Caffeine 的 `ConcurrentHashMap.compute`，但没有任何线程在执行 SQL——连接到底被谁拿着？
2. 打开 Hikari 泄漏检测（`spring.datasource.hikari.leak-detection-threshold=5000`），日志直接给出借出连接的调用栈：全部是 `SelectionLocalCache.studentIdOf → Caffeine loader → selectByUserId`。
3. 看泄漏日志的时间：10 个连接在 06:20:32 借出，**06:21:02.35–02.48 同时归还**——整整 30 s，正好是 `connectionTimeout`。持连接的线程不是在跑 SQL，而是在等另一次借连接超时。

根因是两件事叠加：

1. **`@Transactional(propagation = NOT_SUPPORTED)` 不等于"没有事务"。** 它只是不开真实事务，默认仍会开启事务同步（`SYNCHRONIZATION_ALWAYS`）。MyBatis 通过 `DataSourceUtils.getConnection` 借到的连接会被绑定到线程，直到方法返回才归还——请求在缓存、Redis、MQ 上花的时间都占着连接；里面 REQUIRED 的 `persist` 还会再借第二个连接。
2. **同步缓存的 loader 是在 `ConcurrentHashMap` 的桶锁（bin lock）里执行的。** `cache.get(key, loader)` 为了保证同一个 key 只加载一次，会在 `compute` 持锁期间调用 loader。

于是出现两种相反的锁顺序：线程 A 持有连接、等桶锁（下一次 `get`）；线程 B 持有桶锁、等连接（loader 里查库）。10 个连接全在 A 手里，B 全卡在 loader，A 全卡在 B 的桶锁——死锁，直到 30 s 后 B 借连接超时抛异常、释放桶锁才解开。JVM 的死锁检测只识别 monitor 环，这里混了 `LockSupport.park`，所以 `jstack` 不会报 `Found one Java-level deadlock`。

修复（`SelectionLocalCache` / `StudentService` / `SelectionService.drop`）：

- 本地缓存改为 `AsyncCache`：桶锁里只放一个 `CompletableFuture`，查库交给独立加载线程池（大小 ≤ 连接池 / 2，给请求线程自己的写库留连接）；同一个 key 的并发未命中仍然只加载一次。
- `StudentService` 去掉类级 `@Transactional(readOnly = true)` 和 `NOT_SUPPORTED`：单条查询不开事务，连接在每条 SQL 结束时立即归还；多步写操作才加 `@Transactional`。
- `drop` 在事务内直接用事务连接查 studentId，不占着一个连接去等加载线程借第二个连接。

原则只有一条：**等任何东西的时候，手里不要有数据库连接。**

## API 约定

- 所有响应统一为 `{ "code": 200, "message": "操作成功", "data": ... }`，`code` 与 HTTP 状态码一致。
- 受保护接口需 `Authorization: Bearer <token>`；401 跳登录，403 提示无权限，429 表示触发限流。
- 路径按角色分层：`/api/auth/**`、`/api/public/**`（只读，匿名可访问）、`/api/student/**`、`/api/teacher/**`、`/api/leader/**`、`/api/admin/**`。

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 认证 | POST | `/api/auth/login` | 登录，返回 token + 用户信息 |
| 认证 | POST | `/api/auth/register` | 学生自助注册（忽略传入的角色字段） |
| 门户 | GET | `/api/public/announcements?type=`、`/api/public/news?category=`、`/api/public/courses?semester=&keyword=` | 匿名只读 |
| 学生 | GET/PUT | `/api/student/profile`、`/profile/contact` | 个人信息 / 修改联系方式 |
| 学生 | GET/POST/DELETE | `/api/student/courses[/{id}]` | 我的选课 / 选课 / 退课，选课返回 `{courseId, status, remaining}` |
| 学生 | GET | `/api/student/grades?semester=` | 成绩 |
| 教师 | GET | `/api/teacher/courses`、`/courses/{id}/students`、`/grades` | 授课、名单、成绩 |
| 教师 | PUT | `/api/teacher/courses/{courseId}/grades` | 录入/更新成绩（upsert） |
| 领导 | GET | `/api/leader/statistics/{students\|teachers\|courses}`、`/students`、`/teachers` | 统计与名册 |
| 管理员 | GET/POST/DELETE | `/api/admin/announcements`、`/api/admin/news` | 内容管理 |
| 管理员 | POST/PUT | `/api/admin/courses`、`/api/admin/courses/{id}` | 创建 / 编辑课程（名额上限、选课时间窗口） |
| 管理员 | POST | `/api/admin/cache/courses/refresh` | 以数据库为准重建 Redis 名额缓存 |
| 监控 | GET | `/actuator/health`、`/actuator/prometheus` | 匿名可访问 |

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
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址 | localhost / 6379 |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USER` / `RABBITMQ_PASS` | RabbitMQ 地址 | localhost / 5672 / guest / guest |
| `STOCK_MODE` / `PERSIST_MODE` / `RATE_LIMIT_ENABLED` | 选课链路模式 | db / sync / false |
| `DB_POOL_SIZE` | Hikari 连接池大小；缓存加载线程数上限 = 该值 / 2 | 10 |

Nginx 建议配置：`/` 指向 `frontend/dist`，`/api` 反代到 8080，并开启 gzip（`frontend/nginx.conf` 即为此用途）。

## 重构说明

相对原仓库的主要变化：

**安全**
- 注册接口忽略客户端传入的 `role`，杜绝自助提权为管理员。
- `/api/common/**` 匿名可写的问题修复：只读内容迁到 `/api/public/**`，写操作迁到 `/api/admin/**`。
- 用户实体不再直接序列化（原会泄露 `password` 哈希），统一通过 VO 输出。
- JWT 密钥改配置项 + 环境变量；密钥对象只构建一次；请求头校验通过 `UserDetailsService` 走缓存查找。
- 401 / 403 由 `AuthenticationEntryPoint` / `AccessDeniedHandler` 返回统一 JSON。

**业务**
- 选课名额从"一条原子 UPDATE"升级为可切换的 Redis Lua 预占，记录写入可切换同步 / 线程池 / RabbitMQ；退选后允许再选（复用记录，不触发唯一键冲突）。
- 成绩录入按「学生 + 课程 + 学期」upsert，不再重复插入；校验教师只能给本人课程录分。
- 新增课程选课时间窗口（Flyway V3），请求在本地缓存层就被拦下。

**工程**
- 持久层由 Spring Data JPA 换成 MyBatis-Plus 3.5.18：单表 CRUD 走 `BaseMapper`，联表/统计/动态条件写在 XML（`resultMap + association` 映射嵌套对象，`<where>`/`<if>` 拼条件，`GROUP BY` 聚合到 `GroupCount`），`@Update` 原子更新占/退名额，`MetaObjectHandler` 自动填充 `create_time` / `update_time`。
- 表结构与演示数据交给 Flyway 版本化管理（`db/migration/{vendor}` 方言 + `common` 通用），生产 MySQL 启动即自动迁移，不再依赖 `ddl-auto` / `data.sql`。
- 枚举通过 `@JsonValue` 输出中文标签（`已选`、`优`），数据库仍存枚举名；`role` 保持枚举名以便前端按角色鉴权。
- 异常体系改为 `BusinessException` 子类 + `@RestControllerAdvice`，按语义返回 4xx（含 429）。
- 拆分 `application-{dev,prod,docker}.yml`，测试使用独立的 H2 内存库；需要中间件的用例用 Testcontainers。
- 前端合并 4 个重复 Layout 为一个配置驱动的 `RoleLayout`；axios 拦截器统一处理响应解包、错误提示、401 跳转；修复课表教师名永远为 undefined、删除按钮无事件等问题。

## 路线图

- [x] 引入 Flyway 管理表结构版本
- [x] 高并发选课：Redis 预扣 + MQ 异步落库 + 限流 + 指标 + 压测脚本
- [ ] 列表接口分页（MyBatis-Plus `IPage` + `PaginationInnerInterceptor` 已就绪）与 DTO 校验加强
- [ ] 增加 Refresh Token / 多端登录控制
- [ ] 前端引入单元测试（Vitest + Testing Library）
- [ ] CI：GitHub Actions 跑 `mvnw test` 与 `npm run build`

## License

MIT
