# local-deals-service

这是一个基于 Spring Boot、MyBatis-Plus、MySQL、Redis 的本地生活优惠交易后端项目。项目从黑马点评教程原型重塑为 `local-deals-service`，当前主线是围绕优惠券秒杀交易链路做可靠性、可观测性和压测证据建设。

## 项目亮点

| 教程原型中的边界 | 当前改造 | 证据 |
| --- | --- | --- |
| 秒杀链路主要依赖 Redis Lua 和业务层判断，DB 层缺少最终兜底 | 增加 `tb_voucher_order(user_id, voucher_id)` 唯一索引，并在落库时处理 `DuplicateKeyException` | Flyway 迁移：`src/main/resources/db/migration/`；核心实现：`VoucherOrderServiceImpl#createVoucherOrder` |
| Redis Stream 消费失败后主要依赖 pending-list 重试，失败消息缺少明确归宿 | 增加 pending 重试计数、最大重试次数和 dead-letter Stream | `stream.orders.dlq`、`seckill:stream:retry:*`、消费失败指标 |
| 压测容易只看 HTTP Error%，无法证明业务正确性 | 自动化脚本同时校验 MySQL 订单数、重复下单、DB/Redis 库存、Stream pending 和 dead-letter | `scripts/run-seckill-benchmark.sh`、`docs/JmeterTestSummary/seckill-reliable-v1/metrics.csv` |
| 异步下单链路缺少运行时观测入口 | 接入 Micrometer / Prometheus，暴露请求、消费、重试、死信、pending、DB 幂等等指标 | `/actuator/prometheus` |
| 教程项目名称、包名和数据库痕迹较强 | 项目对外名、Maven 坐标、Spring 应用名、Java 包名和数据库名统一迁移到 `local-deals-service` / `com.localdeals` / `local_deals` | `pom.xml`、`application.yaml`、`db/migration/` |

## 压测摘要

当前可靠性增强版 `reliable-stream-v1` 已完成 4 组 JMeter 压测。每轮均通过业务正确性校验：订单数达到预期、未超卖、无重复下单、Redis Stream pending 为 0、dead-letter 为 0。

| 场景 | 请求配置 | 订单/预期 | P95/P99(ms) | drain_ms | 结论 |
| --- | --- | --- | --- | ---: | --- |
| `seckill-reliable-v1` | 100 线程 / 1 次循环 | 100 / 100 | 3 / 19 | 73 | pass |
| `seckill-reliable-v1` | 1000 线程 / 1 次循环 | 1000 / 1000 | 2 / 29 | 72 | pass |
| `seckill-reliable-v1` | 5000 线程 / 1 次循环 | 1000 / 1000 | 42 / 1116 | 80 | pass |
| `seckill-reliable-v1` | 5000 线程 / 5 次循环 | 1000 / 1000 | 61 / 100 | 76 | pass |

完整结果见 [压测结果记录](docs/benchmark-results.md) 和 `docs/JmeterTestSummary/seckill-reliable-v1/`。

## 技术栈

- Java 8
- Spring Boot 2.3.12
- MyBatis-Plus
- MySQL / Flyway
- Redis / Redis Stream / Redis GEO / Bitmap
- Redisson
- Actuator / Micrometer / Prometheus
- JMeter

## 当前重点

- 登录态：验证码登录后将用户信息写入 Redis Hash，拦截器从 `authorization` 请求头恢复 `UserHolder`。
- 商铺缓存：商铺详情查询结合 Redis 缓存、空值缓存和布隆过滤器，降低无效请求对数据库的压力。
- 优惠券秒杀：Lua 脚本在 Redis 中原子完成库存判断、一人一单判断和订单消息入队，后台消费者批量消费 Redis Stream 后落库。
- 可靠性增强：Flyway 管理表结构迁移，`tb_voucher_order(user_id, voucher_id)` 唯一索引作为一人一单最终兜底；pending 消息有重试上限和死信 Stream。
- 可观测性：暴露秒杀请求、Stream 消费、pending、死信、落库幂等等 Prometheus 指标。
- 附近商铺：使用 Redis GEO 按距离检索商铺，并将距离写回响应对象。

## 文档

- [本地环境与常见问题](docs/environment-setup.md)
- [秒杀压测方案](docs/benchmark-plan.md)
- [JMeter 使用说明](docs/jmeter-usage.md)
- [压测结果记录](docs/benchmark-results.md)
- [项目上下文恢复](docs/project-context.md)

## 本地启动

1. 复制 `.env.example` 为 `.env`，并在 `.env` 中填写本机真实密码。
2. 启动本地 MySQL 和 Redis，默认配置见 `docker-compose.yml`。
3. 新环境推荐使用 Flyway 自动初始化：`src/main/resources/db/migration/`。
4. 如需手工初始化，可参考完整脚本：`src/main/resources/db/local_deals.sql`。
5. 使用 JDK 8 运行项目。
6. 后端默认端口为 `8083`。

```bash
set -a
source .env
set +a
```

```bash
mvn spring-boot:run
```

Docker Compose 会自动读取仓库根目录的 `.env`：

```bash
docker compose up -d
```

Prometheus 指标入口：

```text
http://localhost:8083/actuator/prometheus
```

## 压测准备

压测不绕过正式登录逻辑，也不删除验证码校验。秒杀压测使用测试侧工具预生成测试用户和 Redis token，JMeter 从 CSV 中读取 token 后请求秒杀接口。

推荐使用脚本自动完成测试用户/token 准备、库存重置、JMeter 压测、MySQL/Redis 校验和报告输出：

```bash
set -a
source .env
set +a

scripts/run-seckill-benchmark.sh \
  --threads 100 \
  --loops 1 \
  --stock 100 \
  --user-count 1000 \
  --mysql-container hmdp-mysql \
  --redis-container hmdp-redis
```

不传 `--voucher-id` 时，压测工具会自动创建或复用一张本地压测秒杀券。当前本机复用旧 Docker 容器 `hmdp-mysql` / `hmdp-redis`，但业务库已经切换为 `local_deals`。

更多参数和清理规则见 [秒杀压测方案](docs/benchmark-plan.md)。
