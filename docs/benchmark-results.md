# 压测结果记录

## 记录方式

- 本文件只保留压测总览，不直接粘贴完整 JMeter 输出。
- 后续新增压测优先只向 `结果总览` 追加一行，不再为每一轮压测复制大段详情。
- 每轮完整来源写入 `docs/JmeterTestSummary/<场景名>/<run_id>-run-summary.md`。
- 机器可读总表写入 `docs/JmeterTestSummary/<场景名>/metrics.csv`。
- JMeter 导出的 Summary / Aggregate CSV 作为派生证据保存在 `docs/JmeterTestSummary/<场景名>/` 下。

基线版本执行命令：

```bash
scripts/run-seckill-benchmark.sh --threads 100 --loops 1 --stock 100 --user-count 1000 --voucher-id 11
scripts/run-seckill-benchmark.sh --threads 1000 --loops 1 --stock 1000 --user-count 2000 --voucher-id 11
scripts/run-seckill-benchmark.sh --threads 5000 --loops 1 --stock 1000 --user-count 5000 --voucher-id 11
scripts/run-seckill-benchmark.sh --threads 5000 --loops 5 --stock 1000 --user-count 5000 --voucher-id 11
```

可靠性增强版执行命令：

```bash
scripts/run-seckill-benchmark.sh --threads 100 --loops 1 --stock 100 --user-count 1000 --mysql-container hmdp-mysql --redis-container hmdp-redis
scripts/run-seckill-benchmark.sh --threads 1000 --loops 1 --stock 1000 --user-count 2000 --mysql-container hmdp-mysql --redis-container hmdp-redis
scripts/run-seckill-benchmark.sh --threads 5000 --loops 1 --stock 1000 --user-count 5000 --mysql-container hmdp-mysql --redis-container hmdp-redis
scripts/run-seckill-benchmark.sh --threads 5000 --loops 5 --stock 1000 --user-count 5000 --mysql-container hmdp-mysql --redis-container hmdp-redis
```

当前本机仍复用旧 Docker 容器 `hmdp-mysql` / `hmdp-redis`，但后端连接的业务库是 `local_deals`。不传 `--voucher-id` 时，压测工具会自动创建或复用标题为 `[BENCHMARK] Seckill Voucher` 的本地压测秒杀券，本轮使用 `voucher_id=10`。

## 结果总览

| 日期 | 实现版本 | 场景 | 请求配置 | 库存 | 请求数 | Throughput(samples/sec) | P95/P99(ms) | drain_ms | 订单/预期 | pending | dead-letter | 结论 | 来源 |
| --- | --- | --- | --- | ---: | ---: | ---: | --- | ---: | --- | ---: | ---: | --- | --- |
| 2026-05-16 | baseline-lua-stream | seckill-baseline-lua-stream | 100 线程 / 1 次循环 | 100 | 100 | 20.49180 | 2 / 17 | 76 | 100 / 100 | 0 | 0 | pass | [run-summary](JmeterTestSummary/seckill-baseline-lua-stream/2026-05-16-seckill-baseline-lua-stream-100t-1l-r1-run-summary.md) |
| 2026-05-16 | baseline-lua-stream | seckill-baseline-lua-stream | 1000 线程 / 1 次循环 | 1000 | 1000 | 205.88841 | 2 / 22 | 81 | 1000 / 1000 | 0 | 0 | pass | [run-summary](JmeterTestSummary/seckill-baseline-lua-stream/2026-05-16-seckill-baseline-lua-stream-1000t-1l-r1-run-summary.md) |
| 2026-05-16 | baseline-lua-stream | seckill-baseline-lua-stream | 5000 线程 / 1 次循环 | 1000 | 5000 | 1153.40254 | 155 / 1100 | 78 | 1000 / 1000 | 0 | 0 | pass | [run-summary](JmeterTestSummary/seckill-baseline-lua-stream/2026-05-16-seckill-baseline-lua-stream-5000t-1l-r1-run-summary.md) |
| 2026-05-16 | baseline-lua-stream | seckill-baseline-lua-stream | 5000 线程 / 5 次循环 | 1000 | 25000 | 5120.85211 | 16 / 43 | 75 | 1000 / 1000 | 0 | 0 | pass | [run-summary](JmeterTestSummary/seckill-baseline-lua-stream/2026-05-16-seckill-baseline-lua-stream-5000t-5l-r1-run-summary.md) |
| 2026-05-19 | reliable-stream-v1 | seckill-reliable-v1 | 100 线程 / 1 次循环 | 100 | 100 | 20.53388 | 3 / 19 | 73 | 100 / 100 | 0 | 0 | pass | [run-summary](JmeterTestSummary/seckill-reliable-v1/2026-05-19-seckill-reliable-stream-v1-100t-1l-r3-run-summary.md) |
| 2026-05-19 | reliable-stream-v1 | seckill-reliable-v1 | 1000 线程 / 1 次循环 | 1000 | 1000 | 208.33333 | 2 / 29 | 72 | 1000 / 1000 | 0 | 0 | pass | [run-summary](JmeterTestSummary/seckill-reliable-v1/2026-05-19-seckill-reliable-stream-v1-1000t-1l-r1-run-summary.md) |
| 2026-05-19 | reliable-stream-v1 | seckill-reliable-v1 | 5000 线程 / 1 次循环 | 1000 | 5000 | 1051.96718 | 42 / 1116 | 80 | 1000 / 1000 | 0 | 0 | pass | [run-summary](JmeterTestSummary/seckill-reliable-v1/2026-05-19-seckill-reliable-stream-v1-5000t-1l-r1-run-summary.md) |
| 2026-05-19 | reliable-stream-v1 | seckill-reliable-v1 | 5000 线程 / 5 次循环 | 1000 | 25000 | 5179.20033 | 61 / 100 | 76 | 1000 / 1000 | 0 | 0 | pass | [run-summary](JmeterTestSummary/seckill-reliable-v1/2026-05-19-seckill-reliable-stream-v1-5000t-5l-r1-run-summary.md) |

## 当前观察

- `reliable-stream-v1` 4 组压测正确性均通过：订单数达到预期，未超卖，未重复下单，Redis Stream pending-list 清空，dead-letter 为空。
- `reliable-stream-v1` 的 `drain_ms` 为 72-80 ms，和基线 75-81 ms 基本处在同一量级，说明增加 DB 唯一索引兜底、pending 重试上限、dead-letter 和指标后，没有明显拉长异步落库追平时间。
- `5000 线程 / 1 次循环` 在两个版本中都出现 P99 秒级抖动，可靠性增强版 P95 从 155 ms 降到 42 ms，但 P99 仍为 1116 ms。该场景适合作为压力尖峰观察，不应只用单轮 P99 证明性能提升。
- `5000 线程 / 5 次循环` 的吞吐最高，主要因为库存耗尽后大量请求走库存不足/重复下单的快速拒绝路径，不能简单理解为完整下单能力提升。
- 本轮对外表述重点应放在“可靠性和可观测性增强后，业务正确性仍通过，异步追平耗时保持稳定”，不要夸大为纯性能优化。

## 后续记录方式

后续新增压测时，推荐流程：

1. 使用 `scripts/run-seckill-benchmark.sh` 执行压测。
2. 从脚本输出的 `run-summary.md` 中复制 `Markdown Row`。
3. 将该行追加到本文档的 `结果总览`。
4. 详细参数、JMeter 派生 CSV、JTL、HTML 报告和 Docker 校验结果以 `run-summary.md` 为准。

脚本同时会维护机器可读总表：

```text
docs/JmeterTestSummary/<场景名>/metrics.csv
```
