# 秒杀压测方案

## 目标

压测目标是验证优惠券秒杀链路，不验证短信登录链路。测试请求应直接携带已经生成好的 Redis token，避免验证码、登录和用户创建逻辑干扰秒杀接口的吞吐和延迟指标。

核心观察指标：

- TPS
- 平均响应时间
- P95 / P99 响应时间
- 错误率
- 最终订单数
- 是否超卖
- 是否重复下单
- Redis Stream 是否存在未处理 pending 消息

## 压测数据原则

- 不删除正式登录验证码校验。
- 不新增公开的免验证码登录接口。
- 压测用户使用固定手机号前缀，默认 `1990000xxxx`。
- 压测 token 使用固定格式，默认 `bench-token-{userId}`。
- 每轮压测前必须重置订单、库存、Redis 秒杀 key 和 Stream。

## 准备测试用户和 token

默认生成 1000 个测试用户，并写入 `benchmark/tokens.csv`：

```bash
mvn -Dtest=BenchmarkDataTool#prepareBenchmarkUsersAndTokens test
```

可选参数：

```bash
mvn \
  -Dtest=BenchmarkDataTool#prepareBenchmarkUsersAndTokens \
  -Dbench.userCount=1000 \
  -Dbench.phonePrefix=1990000 \
  -Dbench.tokenTtlMinutes=1440 \
  -Dbench.tokensFile=benchmark/tokens.csv \
  test
```

CSV 不包含表头，每行格式：

```text
token,userId,phone
```

该文件默认不提交到 Git。

## 查看可用秒杀券

如果想查看当前数据库中已有的秒杀券：

```bash
mvn -Dtest=BenchmarkDataTool#listSeckillVouchers test
```

## 重置秒杀数据

默认库存为 `100`。如果不传 `bench.voucherId`，工具会自动创建或复用标题为 `[BENCHMARK] Seckill Voucher` 的本地压测秒杀券：

```bash
mvn -Dtest=BenchmarkDataTool#resetSeckillBenchmarkData test
```

执行成功后，日志中的 `voucherId` 就是 JMeter 请求路径里的 `{voucherId}`。建议在启动后端服务前执行重置，避免运行中的消费者线程正在读取 `stream.orders`。

复用已有秒杀券时，可以显式指定 `bench.voucherId`：

```bash
mvn \
  -Dtest=BenchmarkDataTool#resetSeckillBenchmarkData \
  -Dbench.voucherId=10 \
  -Dbench.stock=100 \
  -Dbench.phonePrefix=1990000 \
  test
```

如果指定的 `bench.voucherId` 在 `tb_seckill_voucher` 中不存在，工具会直接报错，避免误把普通券当作秒杀券压测。

重置内容：

- 删除当前压测手机号前缀用户在目标券上的订单。
- 更新 `tb_seckill_voucher.stock`。
- 写入 `seckill:stock:{voucherId}`。
- 删除 `seckill:order:{voucherId}`。
- 重建 `stream.orders` 和消费者组 `g1`。

## JMeter 配置

详细 GUI 配置、并发参数解释和报告导出方式见 [JMeter 使用说明](jmeter-usage.md)。

当前仓库已保存一个冒烟压测脚本：

```text
docs/Summary Report.jmx
```

这个脚本会随测试场景调整。修改并发时重点检查 `Thread Group` 中的线程数、Ramp-Up 和循环次数；因为文件名包含空格，命令行运行时需要加引号。建议为不同压测场景另存副本，避免覆盖已验证的冒烟脚本或基线脚本。

CSV Data Set Config：

- Filename: `benchmark/tokens.csv`
- Variable Names: `token,userId,phone`
- Recycle on EOF: `True`
- Stop thread on EOF: `False`

HTTP Header Manager：

```text
authorization: ${token}
Content-Type: application/json
```

秒杀接口：

```text
POST http://localhost:8083/voucher-order/seckill/{voucherId}
```

## 命令行运行

冒烟测试：

```bash
mkdir -p benchmark
jmeter -n -t "docs/Summary Report.jmx" -l benchmark/seckill-smoke.jtl
```

生成 HTML 报告：

```bash
rm -rf benchmark/report-smoke
jmeter -n -t "docs/Summary Report.jmx" -l benchmark/seckill-smoke.jtl -e -o benchmark/report-smoke
```

正式压测前建议复制一份脚本，再把线程数、Ramp-Up 和循环次数调大，避免覆盖冒烟脚本。

## 压测后校验

订单数不能超过库存：

```sql
SELECT COUNT(*)
FROM tb_voucher_order
WHERE voucher_id = ${voucherId};
```

同一用户不能重复下单：

```sql
SELECT user_id, COUNT(*) AS cnt
FROM tb_voucher_order
WHERE voucher_id = ${voucherId}
GROUP BY user_id
HAVING cnt > 1;
```

库存不能小于 0：

```sql
SELECT voucher_id, stock
FROM tb_seckill_voucher
WHERE voucher_id = ${voucherId};
```

Redis Stream pending-list 检查：

```bash
redis-cli -a redis123 XPENDING stream.orders g1
```

## 清理压测用户

如果需要删除压测用户和对应 Redis token：

```bash
mvn -Dtest=BenchmarkDataTool#cleanupBenchmarkUsersAndTokens test
```

该操作只删除手机号匹配 `bench.phonePrefix` 的用户及其相关 token。
