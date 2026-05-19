# 项目上下文恢复

本文用于关闭聊天后快速恢复 local-deals-service 项目上下文。它不是聊天流水账，而是记录当前阶段、重要修改、关键结论和下一步计划。

## 当前快照

- 当前分支：`main`
- 最新已推送提交：`511baa6 Automate seckill benchmark reporting`
- 冻结原始基线分支：`baseline-video-version`
- 当前本地阶段：已从教程原型重塑为 `local-deals-service`，并完成秒杀异步下单可靠性增强的第一版代码实现。
- 当前专题：秒杀链路生产化改造、可观测性和压测对比已形成第一版证据。
- 当前实现版本标签：`reliable-stream-v1`
- 本机仓库路径仍是：`/home/sd101t/IdeaProjects/hm-dianping`

## 下一步计划

优先整理“秒杀异步下单可靠性增强”的对外展示和后续补充验证。

1. 将 `README.md`、`docs/benchmark-results.md`、`docs/benchmark-plan.md` 和 `docs/jmeter-usage.md` 保持为当前公开展示入口。
2. 如需继续增强说服力，补一轮故障注入验证：模拟消费失败，确认 pending 重试上限和 `stream.orders.dlq` 行为。
3. 启动后端后检查 `/actuator/prometheus` 中的秒杀指标，必要时补充 Prometheus 指标截图或指标样例。
4. 后续如果新建 `local-deals-mysql` / `local-deals-redis` 容器，再决定是否把脚本默认容器名从旧的 `hmdp-*` 迁移到 `local-deals-*`。

## 最近更新

按时间倒序记录，最新内容放在最前面。每次只记录“做了什么、改了哪些文件、得到什么结论”，不要记录完整聊天过程。

### 2026-05-19

- 使用当前本机旧 Docker 容器 `hmdp-mysql` / `hmdp-redis` 和新业务库 `local_deals` 跑通 `reliable-stream-v1` 四组压测。
- `reliable-stream-v1` 结果已写入 `docs/JmeterTestSummary/seckill-reliable-v1/` 和 `docs/benchmark-results.md`：100、1000、5000、25000 请求四组均 `pass`，pending 为 0，dead-letter 为 0，`drain_ms` 为 72-80 ms。
- 当前压测命令需要显式传入 `--mysql-container hmdp-mysql --redis-container hmdp-redis`；不传 `--voucher-id`，由工具自动创建或复用 `local_deals` 中的 benchmark 秒杀券。
- README 增加“项目亮点 / 相比教程版的改造”和可靠性增强版压测摘要，用于对外展示。
- 项目对外名重塑为 `local-deals-service`，Maven 坐标、Spring 应用名、Java 包名和主类迁移到 `com.localdeals` / `LocalDealsApplication`。
- 引入 Flyway，新增 `db/migration/V1__baseline_schema.sql` 和 `V2__voucher_order_constraints.sql`，用唯一索引作为一人一单的 DB 最终兜底。
- 改造 `VoucherOrderServiceImpl`：Redis Stream key/group/consumer/read-count/worker-count/max-retry/dead-letter 配置化，消费者支持批量读取、pending 重试上限和 dead-letter Stream。
- 落库流程改为先插入订单触发唯一索引幂等兜底，再扣减 DB 库存；库存扣减失败会抛异常回滚订单插入。
- 引入 Micrometer Prometheus Registry，暴露秒杀请求结果、消费成功/失败/重试/死信、DB 幂等冲突、Stream 长度和 pending 等指标。
- 更新 `scripts/run-seckill-benchmark.sh`，默认新场景为 `seckill-reliable-v1`，并把 dead-letter 数纳入正确性校验。
- 完成配置脱敏：真实 DB/Redis 密码改为通过 `.env` 或环境变量注入，仓库只提交 `.env.example`。
- 已执行 `/opt/idea/plugins/maven/lib/maven3/bin/mvn clean -DskipTests package`，打包通过。

### 2026-05-16

- 建立秒杀压测自动化流程，脚本统一执行测试用户/token 准备、秒杀数据重置、JMeter 压测、MySQL/Redis 校验、`drain_ms` 测量和报告输出。
- 参数化 `docs/Summary Report.jmx`，支持通过脚本参数控制线程数、循环次数、库存、用户数、优惠券 ID 等压测变量。
- 清理旧的手工压测文件，只保留当前脚本生成的结果入口和证据文件。
- 更新 `docs/benchmark-results.md`，将它定位为人工阅读总览表，不再堆积所有原始输出。
- 已提交并推送压测自动化相关改动：`511baa6 Automate seckill benchmark reporting`。
- 新增本文档作为后续聊天恢复入口。

## 当前专题：秒杀压测与优化

### 已执行的基线压测

```bash
scripts/run-seckill-benchmark.sh --threads 100 --loops 1 --stock 100 --user-count 1000 --voucher-id 11
scripts/run-seckill-benchmark.sh --threads 1000 --loops 1 --stock 1000 --user-count 2000 --voucher-id 11
scripts/run-seckill-benchmark.sh --threads 5000 --loops 1 --stock 1000 --user-count 5000 --voucher-id 11
scripts/run-seckill-benchmark.sh --threads 5000 --loops 5 --stock 1000 --user-count 5000 --voucher-id 11
```

结果以 `docs/benchmark-results.md` 和 `docs/JmeterTestSummary/seckill-baseline-lua-stream/metrics.csv` 为准。

### 当前观察

- baseline 与 `reliable-stream-v1` 两组结果均通过业务正确性校验。
- `reliable-stream-v1` 增加 DB 唯一索引兜底、pending 重试上限、dead-letter 和指标后，`drain_ms` 为 72-80 ms，和 baseline 的 75-81 ms 处在同一量级。
- `5000 线程 / 1 次循环` 仍然有 P99 秒级抖动，适合作为压力尖峰观察，不宜单独作为性能提升论据。
- `5000 线程 / 5 次循环` 吞吐较高，主要因为库存耗尽后大量请求走快速拒绝路径，不能简单理解为完整下单能力提升。

### 结果入口

- `docs/benchmark-results.md`
  - 人工阅读入口，只保留压测总览表和简短观察。
- `docs/JmeterTestSummary/seckill-baseline-lua-stream/metrics.csv`
  - 机器可读总表，每轮脚本压测自动更新。
- `docs/JmeterTestSummary/seckill-reliable-v1/metrics.csv`
  - 可靠性增强版机器可读总表。
- `docs/JmeterTestSummary/seckill-baseline-lua-stream/*-run-summary.md`
  - 每轮压测的完整证据快照。
- `docs/JmeterTestSummary/seckill-reliable-v1/*-run-summary.md`
  - 可靠性增强版每轮压测的完整证据快照。
- `summary.csv` 和 `aggregate.csv`
  - JMeter 派生证据，不是主要阅读入口。

## 关键文件

- `scripts/run-seckill-benchmark.sh`
  - 自动执行测试用户/token 准备、秒杀数据重置、JMeter 压测、MySQL/Redis 校验、`drain_ms` 测量、CSV/HTML/summary 输出。
- `docs/Summary Report.jmx`
  - JMeter 脚本已参数化，支持通过 `-Jthreads`、`-Jloops`、`-JvoucherId`、`-JtokensFile` 等参数运行。
- `docs/jmeter-usage.md`
  - 记录自动化脚本用法、参数含义、输出文件命名和 `drain_ms` 含义。
- `docs/benchmark-results.md`
  - 压测结果人工总览。
- `src/main/java/com/localdeals/service/impl/VoucherOrderServiceImpl.java`
  - 秒杀 Lua 判定、Redis Stream 入队、异步消费、pending/dead-letter 和落库幂等的核心实现位置。
- `src/main/java/com/localdeals/config/SeckillProperties.java`
  - 秒杀 Stream 消费配置入口。
- `src/main/resources/db/migration/`
  - Flyway schema 初始化和约束迁移。

## 维护规则

- 新聊天产生的重要决策或修改，追加到“最近更新”，并保持最新日期在前。
- 每次更新优先记录结论、文件路径和下一步，不记录完整对话。
- 如果某个专题变大，可以在“当前专题”下新增二级标题，例如“商铺缓存优化”“登录鉴权整理”。
- `benchmark/` 下的 JTL、HTML 报告、token 和 reset log 默认不提交。
- 终端中没有系统 `mvn` 时，脚本会使用 IDEA 内置 Maven：`/opt/idea/plugins/maven/lib/maven3/bin/mvn`。
- 脚本默认使用 Dragonwell JDK 8：`/home/sd101t/.jdks/dragonwell-ex-1.8.0_472`。

## 重启提示

后续新聊天可以直接粘贴：

```text
请先阅读 /home/sd101t/IdeaProjects/hm-dianping/docs/project-context.md，
再结合当前专题中列出的关键文件恢复 local-deals-service 项目上下文。
当前 reliable-stream-v1 已完成四组压测并写入 docs/benchmark-results.md。
后续重点是整理对外展示、补充 Prometheus 指标确认，或做 pending 重试/dead-letter 的故障注入验证。
当前本机压测命令需要显式传入 --mysql-container hmdp-mysql --redis-container hmdp-redis。
```
