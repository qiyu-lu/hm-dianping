# 压测结果记录

## 2026-05-15 教程原版基线

### 场景

- 日期：2026-05-15
- Git commit：`4e6f682`（本次记录时的 HEAD，工作区存在未提交改动）
- 分支：`main`
- 实现版本：教程原版 Redis Lua + Redis Stream 异步下单
- JMeter 脚本：`docs/Summary Report.jmx`
- 历史冒烟结果：`docs/2026-05-15-seckill-baseline-lua-stream-100t-summary.csv`
- 本轮正式结果：
  - `docs/JmeterTestSummary/seckill-baseline-lua-stream/2026-05-15-seckill-baseline-lua-stream-100t-summary-1.csv`
  - `docs/JmeterTestSummary/seckill-baseline-lua-stream/2026-05-15-seckill-baseline-lua-stream-100t-aggregate-1.csv`
- 接口：`POST /voucher-order/seckill/${voucherId}`
- voucherId：`11`
- 初始库存：`100`
- JMeter 线程数：`100`
- Ramp-Up：`5s`
- Loop Count：`1`
- CSV token 文件：`benchmark/tokens.csv`

### JMeter Summary Report

| 版本 | 并发数 | 请求数 | Throughput(samples/sec) | 平均响应(ms) | Min(ms) | Max(ms) | Std. Dev. | 错误率 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 教程原版 Lua + Stream | 100 | 100 | 20.19386 | 2 | 1 | 6 | 0.58 | 0.000% |

> 说明：本轮重新清空 Listener 后导出，Summary Report 与 Aggregate Report 的样本数均为 100，可以作为同一轮压测记录。

### JMeter Aggregate Report

| 版本 | 并发数 | 请求数 | Throughput(samples/sec) | 平均响应(ms) | Median(ms) | P90(ms) | P95(ms) | P99(ms) | Min(ms) | Max(ms) | 错误率 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 教程原版 Lua + Stream | 100 | 100 | 20.19386 | 2 | 2 | 3 | 3 | 3 | 1 | 6 | 0.000% |

### 正确性校验

| 校验项 | 期望 | 实际 | 是否通过 |
| --- | --- | --- | --- |
| 有效下单数 | 库存和用户充足时应 `> 0` | 100 条订单 | 通过 |
| 订单数不超过初始库存 | `订单数 <= 初始库存` | 100 条订单，初始库存 100 | 通过 |
| 不存在同一用户重复下单 | 0 条 | 0 条 | 通过 |
| DB 库存不为负数 | `stock >= 0` | 0 | 通过 |
| Redis 库存扣减结果 | `seckill:stock:11 = 0` | 0 | 通过 |
| Redis 已购用户集合 | `SCARD seckill:order:11 = 100` | 100 | 通过 |
| Stream 消息数量 | 约 101 条，包含 1 条初始化消息 | 101 | 通过 |
| Stream pending-list 清空 | 0 | 0 | 通过 |

### 校验 SQL

```sql
SELECT COUNT(*)
FROM tb_voucher_order
WHERE voucher_id = 11;
```

```sql
SELECT user_id, COUNT(*) AS cnt
FROM tb_voucher_order
WHERE voucher_id = 11
GROUP BY user_id
HAVING cnt > 1;
```

```sql
SELECT voucher_id, stock
FROM tb_seckill_voucher
WHERE voucher_id = 11;
```

Redis Stream pending-list：

```bash
redis-cli -a redis123 GET seckill:stock:11
redis-cli -a redis123 SCARD seckill:order:11
redis-cli -a redis123 XLEN stream.orders
redis-cli -a redis123 XPENDING stream.orders g1
```

### 当前结论

- 这轮可以作为“教程原版 Lua + Stream 秒杀链路”的第一条基线记录。
- JMeter 层面没有 HTTP 错误，说明 token 读取、鉴权、秒杀接口路径和基础链路已经跑通。
- 本轮初始库存 100，最终 DB 订单数 100、Redis 库存为 0、Redis 已购集合为 100，未出现超卖。
- 重复下单校验结果为空，说明一人一单约束在本轮压测中生效。
- `stream.orders` 长度为 101，其中包含 1 条初始化消息；pending-list 为 0，说明本轮消息已经被消费者确认。
- Aggregate Report 显示 P95 为 3 ms、P99 为 3 ms；该数据可作为后续优化对比中的延迟基线。
- 后续正式对比时继续保持“每轮清空 Listener 或使用命令行生成独立结果文件”，避免历史运行累计影响样本数。

## 2026-05-15 教程原版超卖压力测试

### 场景

- 日期：2026-05-15
- Git commit：`4e6f682`（本次记录时的 HEAD，工作区存在未提交改动）
- 分支：`main`
- 实现版本：教程原版 Redis Lua + Redis Stream 异步下单
- JMeter 脚本：`docs/Summary Report.jmx`
- 本轮正式结果：
  - `docs/JmeterTestSummary/seckill-baseline-lua-stream/2026-05-15-seckill-baseline-lua-stream-500t-2l-summary.csv`
  - `docs/JmeterTestSummary/seckill-baseline-lua-stream/2026-05-15-seckill-baseline-lua-stream-500t-2l-aggregate.csv`
- 接口：`POST /voucher-order/seckill/${voucherId}`
- voucherId：`11`
- 初始库存：`100`
- JMeter 线程数：`500`
- Ramp-Up：`5s`
- Loop Count：`2`
- 总请求数：`1000`
- CSV token 文件：`benchmark/tokens.csv`

### JMeter Summary Report

| 版本 | 并发数 | Loop Count | 请求数 | Throughput(samples/sec) | 平均响应(ms) | Min(ms) | Max(ms) | Std. Dev. | 错误率 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 教程原版 Lua + Stream | 500 | 2 | 1000 | 200.36065 | 0 | 0 | 3 | 0.42 | 0.000% |

### JMeter Aggregate Report

| 版本 | 并发数 | Loop Count | 请求数 | Throughput(samples/sec) | 平均响应(ms) | Median(ms) | P90(ms) | P95(ms) | P99(ms) | Min(ms) | Max(ms) | 错误率 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 教程原版 Lua + Stream | 500 | 2 | 1000 | 200.36065 | 0 | 1 | 1 | 1 | 2 | 0 | 3 | 0.000% |

### 正确性校验

| 校验项 | 期望 | 实际 | 是否通过 |
| --- | --- | --- | --- |
| 有效下单数 | 不超过初始库存 100 | 100 条订单 | 通过 |
| 订单数不超过初始库存 | `订单数 <= 初始库存` | 100 条订单，初始库存 100 | 通过 |
| 不存在同一用户重复下单 | 0 条 | 0 条 | 通过 |
| DB 库存不为负数 | `stock >= 0` | 0 | 通过 |
| Redis 库存扣减结果 | `seckill:stock:11 = 0` | 0 | 通过 |
| Redis 已购用户集合 | `SCARD seckill:order:11 = 100` | 100 | 通过 |
| Stream 消息数量 | 约 101 条，包含 1 条初始化消息 | 101 | 通过 |
| Stream pending-list 清空 | 0 | 0 | 通过 |

### 当前结论

- 本轮是库存 100 下的 500 线程、每线程 2 次请求压力测试，总请求数 1000。
- 最终 DB 订单数为 100，Redis 库存为 0，Redis 已购集合为 100，未出现超卖。
- 重复下单校验结果为空，说明在重复请求和库存耗尽场景下，一人一单约束仍然生效。
- Stream pending-list 为 0，说明本轮入队成功的订单消息已被消费者确认。
- JMeter HTTP 错误率为 0.000%，但本轮 1000 次请求中只有 100 次会形成有效订单，其余请求属于库存不足或重复下单等业务拒绝；该轮更适合作为“超卖压力正确性验证”，不应和 100 并发全成功场景直接比较业务成功率。

## 后续记录模板

### 场景

- 日期：
- Git commit：
- 分支：
- 实现版本：
- JMeter 脚本：
- 原始结果：
- voucherId：
- 初始库存：
- 测试用户数：
- JMeter 线程数：
- Ramp-Up：
- Loop Count：

### JMeter 结果

| 版本 | 并发数 | 请求数 | Throughput(samples/sec) | 平均响应(ms) | P95(ms) | P99(ms) | 错误率 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
|  |  |  |  |  |  |  |  |

### 正确性校验

| 校验项 | 期望 | 实际 | 是否通过 |
| --- | --- | --- | --- |
| 订单数不超过初始库存 |  |  |  |
| 不存在同一用户重复下单 | 0 条 |  |  |
| DB 库存不为负数 | `stock >= 0` |  |  |
| Stream pending-list 清空 | 0 |  |  |

### 结论

- 性能变化：
- 正确性结论：
- 当前瓶颈：
- 下一步改进：
