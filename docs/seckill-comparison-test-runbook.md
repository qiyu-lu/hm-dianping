# 秒杀对比验证执行手册

本文记录“原始教程版 baseline”与“当前可靠性增强版 reliable-stream-v1”的对比验证步骤。目标是让后续聊天上下文丢失时，也能按本文继续跑测试、补证据、更新结果文档。

## 验证目标

这轮测试不再证明“功能能跑”，而是为简历和面试表述补充可追溯证据：

- baseline：保留黑马点评原始秒杀链路的行为边界。
- reliable-stream-v1：验证增加 DB 唯一索引兜底、pending 重试上限、dead-letter Stream 和指标后，正常压测正确性不退化，异常消息可以闭环处理。
- 对外表述重点：可靠性、可观测性、自动化验证增强；不要把单轮吞吐或 P99 波动包装成显著性能提升。

## 路径和版本

| 用途 | 路径 / 分支 | 说明 |
| --- | --- | --- |
| 当前主仓库 | `/home/sd101t/IdeaProjects/hm-dianping`，`main` | 保存脚本、JMeter 计划、所有测试证据和最终文档。 |
| baseline worktree | `/home/sd101t/IdeaProjects/hm-dianping-baseline`，`feature-seckill-benchmark-baseline` | 只负责运行旧代码，不在里面沉淀最终证据。 |
| 原始冻结点 | `baseline-video-version` | 更早的教程原始点；当前测试用的 baseline worktree 已补入测试夹具，便于压测。 |

不要通过 `git switch` 在主仓库来回切换版本。使用 worktree 的原因是：

- 主仓库可以一直保留当前脚本和输出目录，避免切分支时覆盖证据。
- baseline 代码和当前代码可以分别启动，出问题时容易定位是旧实现还是新实现。
- baseline 的旧 JMeter 文件存在硬编码 token 路径风险，统一使用主仓库参数化后的 `docs/Summary Report.jmx` 更稳。

如果 baseline worktree 丢失，可从主仓库重新创建：

```bash
git worktree add /home/sd101t/IdeaProjects/hm-dianping-baseline feature-seckill-benchmark-baseline
```

## 环境约束

本机当前复用旧 Docker 容器，但两个版本连接的数据库不同：

| 版本 | MySQL 容器 | 数据库 | Redis 容器 |
| --- | --- | --- | --- |
| baseline-lua-stream | `hmdp-mysql` | `hmdp` | `hmdp-redis` |
| reliable-stream-v1 | `hmdp-mysql` | `local_deals` | `hmdp-redis` |

注意事项：

- 两个版本共用同一个 Redis 容器，切换版本前必须通过脚本重置 `stream.orders`、`stream.orders.dlq` 和 `seckill:stream:retry:*`。
- 每次只启动一个后端服务，因为两边默认都监听 `8083`。
- 主仓库脚本需要读取 `.env` 中的 MySQL/Redis 校验密码，但不要把真实密码写入文档。
- 当前机器可用 Maven：`/opt/idea/plugins/maven/lib/maven3/bin/mvn`。
- 当前机器默认 JDK：`/home/sd101t/.jdks/dragonwell-ex-1.8.0_472`。

通用准备：

```bash
cd /home/sd101t/IdeaProjects/hm-dianping
set -a
source .env
set +a
export JAVA_HOME=/home/sd101t/.jdks/dragonwell-ex-1.8.0_472
export PATH="$JAVA_HOME/bin:$PATH"
```

## 启动服务

### 启动 baseline

```bash
cd /home/sd101t/IdeaProjects/hm-dianping-baseline
/opt/idea/plugins/maven/lib/maven3/bin/mvn spring-boot:run
```

baseline 自身配置连接 `hmdp` 数据库。脚本侧仍然从主仓库 `.env` 读取 Docker 校验密码。

### 启动当前版本

```bash
cd /home/sd101t/IdeaProjects/hm-dianping
set -a
source .env
set +a
/opt/idea/plugins/maven/lib/maven3/bin/mvn spring-boot:run
```

当前版本通过环境变量连接 `local_deals` 数据库。

## 正常压测命令

所有输出都写回主仓库 `/home/sd101t/IdeaProjects/hm-dianping`。

### baseline：1000 线程 / 1 次循环

```bash
cd /home/sd101t/IdeaProjects/hm-dianping
set -a
source .env
set +a
scripts/run-seckill-benchmark.sh \
  --project-dir /home/sd101t/IdeaProjects/hm-dianping-baseline \
  --output-root /home/sd101t/IdeaProjects/hm-dianping \
  --jmeter-plan /home/sd101t/IdeaProjects/hm-dianping/docs/Summary\ Report.jmx \
  --scenario seckill-baseline-lua-stream \
  --impl baseline-lua-stream \
  --threads 1000 \
  --loops 1 \
  --stock 1000 \
  --user-count 2000 \
  --voucher-id 11 \
  --mysql-container hmdp-mysql \
  --redis-container hmdp-redis \
  --mysql-database hmdp \
  --skip-html
```

### baseline：5000 线程 / 1 次循环

```bash
cd /home/sd101t/IdeaProjects/hm-dianping
set -a
source .env
set +a
scripts/run-seckill-benchmark.sh \
  --project-dir /home/sd101t/IdeaProjects/hm-dianping-baseline \
  --output-root /home/sd101t/IdeaProjects/hm-dianping \
  --jmeter-plan /home/sd101t/IdeaProjects/hm-dianping/docs/Summary\ Report.jmx \
  --scenario seckill-baseline-lua-stream \
  --impl baseline-lua-stream \
  --threads 5000 \
  --loops 1 \
  --stock 1000 \
  --user-count 5000 \
  --voucher-id 11 \
  --mysql-container hmdp-mysql \
  --redis-container hmdp-redis \
  --mysql-database hmdp \
  --skip-html
```

### reliable-stream-v1：1000 线程 / 1 次循环

```bash
cd /home/sd101t/IdeaProjects/hm-dianping
set -a
source .env
set +a
scripts/run-seckill-benchmark.sh \
  --scenario seckill-reliable-v1 \
  --impl reliable-stream-v1 \
  --threads 1000 \
  --loops 1 \
  --stock 1000 \
  --user-count 2000 \
  --mysql-container hmdp-mysql \
  --redis-container hmdp-redis \
  --mysql-database local_deals \
  --skip-html
```

### reliable-stream-v1：5000 线程 / 1 次循环

```bash
cd /home/sd101t/IdeaProjects/hm-dianping
set -a
source .env
set +a
scripts/run-seckill-benchmark.sh \
  --scenario seckill-reliable-v1 \
  --impl reliable-stream-v1 \
  --threads 5000 \
  --loops 1 \
  --stock 1000 \
  --user-count 5000 \
  --mysql-container hmdp-mysql \
  --redis-container hmdp-redis \
  --mysql-database local_deals \
  --skip-html
```

## 故障注入命令

故障注入脚本会重置压测数据，然后向 `stream.orders` 写入缺少 `orderId` 的异常消息：

```text
userId=<fault_user_id>, voucherId=<voucher_id>, missing orderId
```

baseline 的预期是消息残留在 pending-list；当前版本的预期是 pending 被清空，并进入 `stream.orders.dlq`。

### baseline 故障注入

```bash
cd /home/sd101t/IdeaProjects/hm-dianping
set -a
source .env
set +a
scripts/run-seckill-reliability-check.sh \
  --project-dir /home/sd101t/IdeaProjects/hm-dianping-baseline \
  --output-root /home/sd101t/IdeaProjects/hm-dianping \
  --impl baseline-lua-stream \
  --expect baseline \
  --mysql-container hmdp-mysql \
  --redis-container hmdp-redis \
  --mysql-database hmdp \
  --voucher-id 11
```

通过标准：

- `stream_pending > 0`
- `stream_dead_letters = 0`
- `correctness = pass`

### reliable-stream-v1 故障注入

```bash
cd /home/sd101t/IdeaProjects/hm-dianping
set -a
source .env
set +a
scripts/run-seckill-reliability-check.sh \
  --impl reliable-stream-v1 \
  --expect current \
  --mysql-container hmdp-mysql \
  --redis-container hmdp-redis \
  --mysql-database local_deals
```

通过标准：

- `stream_pending = 0`
- `stream_dead_letters > 0`
- `correctness = pass`

## 必须记录的证据

正常压测每轮至少记录：

- `run_id`
- 实现版本、场景、线程数、循环数、库存、用户数、请求数
- `throughput`
- `avg_ms`、`p95_ms`、`p99_ms`、`max_ms`
- `error_pct`
- `jmeter_elapsed_ms`
- `drain_ms`
- `mysql_orders / expected_orders`
- `duplicate_orders`
- `mysql_stock`
- `redis_stock`
- `redis_order_count`
- `stream_len`
- `stream_pending`
- `stream_dead_letters`
- `correctness`
- `run_summary`、`jtl_file`、`summary_csv`、`aggregate_csv`

故障注入每轮至少记录：

- `run_id`
- 实现版本和预期类型：`baseline` 或 `current`
- `voucher_id`
- `injected_record_id`
- 异常载荷说明：缺少 `orderId`
- `stream_pending`
- `stream_dead_letters`
- `retry_key_count`
- `correctness`
- dead-letter 样例，当前版本应能看到被转存的异常消息

人工总览只需要从每个 `run-summary.md` 的 `Markdown Row` 复制到：

- 正常压测：`docs/benchmark-results.md`
- 故障注入：建议新建或更新 `docs/reliability-results.md`

机器可读总表由脚本自动维护：

- `docs/JmeterTestSummary/seckill-baseline-lua-stream/metrics.csv`
- `docs/JmeterTestSummary/seckill-reliable-v1/metrics.csv`
- `docs/reliability-results/metrics.csv`

## 正确性判定

正常压测通过条件：

- JMeter `error_pct = 0.000%`
- MySQL 订单数等于 `expected_orders`
- 重复下单数 `duplicate_orders = 0`
- MySQL 库存不小于 0
- Redis 库存等于 `stock - expected_orders`
- Redis 已下单用户集合数量等于 `expected_orders`
- Redis Stream pending 为 0
- dead-letter 为 0

故障注入通过条件：

- baseline：异常消息没有闭环能力，表现为 pending 残留且 DLQ 为空。
- reliable-stream-v1：异常消息经过有限重试后进入 DLQ，pending 清空。

## 当前完成状态

截至 2026-05-20，本轮 baseline 与 `reliable-stream-v1` 对比验证已补齐：

| 状态 | 版本 | 场景 | run_id / 说明 |
| --- | --- | --- | --- |
| 已完成且有效 | baseline-lua-stream | 1000 线程 / 1 次循环 | `2026-05-20-seckill-baseline-lua-stream-1000t-1l-r3` |
| 已完成且有效 | baseline-lua-stream | 5000 线程 / 1 次循环 | `2026-05-20-seckill-baseline-lua-stream-5000t-1l-r1` |
| 已完成且有效 | baseline-lua-stream | 故障注入 | `2026-05-20-seckill-baseline-lua-stream-fault-injection-r1` |
| 已完成且有效 | reliable-stream-v1 | 1000 线程 / 1 次循环 | `2026-05-20-seckill-reliable-stream-v1-1000t-1l-r1` |
| 已完成且有效 | reliable-stream-v1 | 5000 线程 / 1 次循环 | `2026-05-20-seckill-reliable-stream-v1-5000t-1l-r1` |
| 已完成且有效 | reliable-stream-v1 | 故障注入 | `2026-05-20-seckill-reliable-stream-v1-fault-injection-r1` |
| 已标记废弃 | baseline-lua-stream | 1000 线程 / 1 次循环 | `r1`、`r2` 使用了 baseline 旧 JMX 的硬编码 token 路径，出现 2 个 401 和 998/1000 订单，不应作为有效对比证据 |

后续恢复时，建议按下面顺序继续：

1. 读取 `docs/benchmark-results.md` 和 `docs/reliability-results.md`，不要重复跑已经有效的 2026-05-20 证据。
2. 若要重新验证，再确认没有旧服务占用 `8083`，只启动一个版本服务。
3. 重跑时继续按本文命令显式指定 `--mysql-container hmdp-mysql --redis-container hmdp-redis --mysql-database <hmdp|local_deals>`。
4. 执行 `bash -n scripts/run-seckill-benchmark.sh scripts/run-seckill-reliability-check.sh`。
5. 用 `git status -sb` 检查最终变更范围。

## 简历表述边界

可以说：

- 将教程版 Redis Stream 异步下单改造为带 DB 唯一索引兜底、pending 重试上限、dead-letter 和 Prometheus 指标的可靠消费链路。
- 搭建 JMeter + MySQL + Redis 校验脚本，自动记录吞吐、P95/P99、异步落库追平时间、订单一致性、pending 和 dead-letter 状态。
- 用异常消息注入验证原始实现会产生 pending 残留，改造后能进入 DLQ 并清空 pending。

不要说：

- “性能大幅提升”。
- “完全生产可用”。
- “解决了所有秒杀一致性问题”。

更稳妥的说法是：在保持正常压测正确性和异步追平耗时同量级的前提下，补齐了异常消费闭环和可观测证据。
