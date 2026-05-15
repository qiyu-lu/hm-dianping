# hm-dianping

这是一个基于 Spring Boot、MyBatis-Plus、MySQL、Redis 的本地生活服务后端项目。当前重点围绕 Redis 缓存、优惠券秒杀、异步下单和附近商铺查询等后端能力进行学习、改造和压测验证。

## 技术栈

- Java 8
- Spring Boot 2.3.12
- MyBatis-Plus
- MySQL
- Redis / Redis Stream / Redis GEO / Bitmap
- Redisson
- JMeter

## 当前重点

- 登录态：验证码登录后将用户信息写入 Redis Hash，拦截器从 `authorization` 请求头恢复 `UserHolder`。
- 商铺缓存：商铺详情查询结合 Redis 缓存、空值缓存和布隆过滤器，降低无效请求对数据库的压力。
- 优惠券秒杀：Lua 脚本在 Redis 中原子完成库存判断、一人一单判断和订单消息入队，后台线程消费 Redis Stream 后落库。
- 附近商铺：使用 Redis GEO 按距离检索商铺，并将距离写回响应对象。

## 文档

- [本地环境与常见问题](docs/environment-setup.md)
- [秒杀压测方案](docs/benchmark-plan.md)
- [JMeter 使用说明](docs/jmeter-usage.md)
- [压测结果记录](docs/benchmark-results.md)

## 本地启动

1. 启动本地 MySQL 和 Redis，默认配置见 `src/main/resources/application.yaml`。
2. 初始化数据库脚本：`src/main/resources/db/hmdp.sql`。
3. 使用 JDK 8 运行项目。
4. 后端默认端口为 `8083`。

```bash
mvn spring-boot:run
```

## 压测准备

压测不绕过正式登录逻辑，也不删除验证码校验。秒杀压测使用测试侧工具预生成测试用户和 Redis token，JMeter 从 CSV 中读取 token 后请求秒杀接口。

常用命令：

```bash
mvn -Dtest=BenchmarkDataTool#prepareBenchmarkUsersAndTokens test
mvn -Dtest=BenchmarkDataTool#resetSeckillBenchmarkData test
```

不传 `bench.voucherId` 时，压测工具会自动创建或复用一张本地压测秒杀券。

更多参数和清理规则见 [秒杀压测方案](docs/benchmark-plan.md)。
