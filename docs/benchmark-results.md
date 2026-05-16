# 压测结果记录

## 记录方式

- 本文件只保留压测总览，不直接粘贴完整 JMeter 输出。
- 后续新增压测优先只向 `结果总览` 追加一行，不再为每一轮压测复制大段详情。
- 每轮完整来源写入 `docs/JmeterTestSummary/<场景名>/<run_id>-run-summary.md`。
- 机器可读总表写入 `docs/JmeterTestSummary/<场景名>/metrics.csv`。
- JMeter 导出的 Summary / Aggregate CSV 作为派生证据保存在 `docs/JmeterTestSummary/<场景名>/` 下。

执行了命令：

```bash
scripts/run-seckill-benchmark.sh --threads 100 --loops 1 --stock 100 --user-count 1000 --voucher-id 11
scripts/run-seckill-benchmark.sh --threads 1000 --loops 1 --stock 1000 --user-count 2000 --voucher-id 11
scripts/run-seckill-benchmark.sh --threads 5000 --loops 1 --stock 1000 --user-count 5000 --voucher-id 11
scripts/run-seckill-benchmark.sh --threads 5000 --loops 5 --stock 1000 --user-count 5000 --voucher-id 11
```

## 结果总览

| 日期 | 实现版本 | 场景 | 请求配置 | 库存 | 请求数 | Throughput(samples/sec) | P95/P99(ms) | drain_ms | 订单/预期 | pending | 结论 | 来源 |
| --- | --- | --- | --- | ---: | ---: | ---: | --- | ---: | --- | ---: | --- | --- |
| 2026-05-16 | baseline-lua-stream | seckill-baseline-lua-stream | 100 线程 / 1 次循环 | 100 | 100 | 20.49180 | 2 / 17 | 76 | 100 / 100 | 0 | pass | [run-summary](JmeterTestSummary/seckill-baseline-lua-stream/2026-05-16-seckill-baseline-lua-stream-100t-1l-r1-run-summary.md) |
| 2026-05-16 | baseline-lua-stream | seckill-baseline-lua-stream | 1000 线程 / 1 次循环 | 1000 | 1000 | 205.88841 | 2 / 22 | 81 | 1000 / 1000 | 0 | pass | [run-summary](JmeterTestSummary/seckill-baseline-lua-stream/2026-05-16-seckill-baseline-lua-stream-1000t-1l-r1-run-summary.md) |
| 2026-05-16 | baseline-lua-stream | seckill-baseline-lua-stream | 5000 线程 / 1 次循环 | 1000 | 5000 | 1153.40254 | 155 / 1100 | 78 | 1000 / 1000 | 0 | pass | [run-summary](JmeterTestSummary/seckill-baseline-lua-stream/2026-05-16-seckill-baseline-lua-stream-5000t-1l-r1-run-summary.md) |
| 2026-05-16 | baseline-lua-stream | seckill-baseline-lua-stream | 5000 线程 / 5 次循环 | 1000 | 25000 | 5120.85211 | 16 / 43 | 75 | 1000 / 1000 | 0 | pass | [run-summary](JmeterTestSummary/seckill-baseline-lua-stream/2026-05-16-seckill-baseline-lua-stream-5000t-5l-r1-run-summary.md) |

## 当前观察

- 4 组压测正确性均通过：订单数达到预期，未超卖，未重复下单，Redis Stream pending-list 清空。
- `drain_ms` 稳定在 75-81 ms，说明本轮异步消费者在 JMeter 请求结束后很快追平 MySQL 落库。
- `5000 线程 / 1 次循环` 的 P95/P99 明显高于其他组，后续如果要作为核心对比基线，建议复跑一次同场景确认是否为瞬时抖动。
- `5000 线程 / 5 次循环` 吞吐最高且 P99 较低，主要因为库存耗尽后大量请求走库存不足/重复下单的快速拒绝路径，不能简单理解为完整下单能力提升。

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
