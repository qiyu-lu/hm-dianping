--[[
秒杀 Lua 脚本（KEYS 派，推荐版）

功能：
1. 判断库存是否充足
2. 判断用户是否已抢过
3. 扣减库存并记录用户

参数说明：
KEYS[1] - 库存 key，例如：seckill:stock:17
KEYS[2] - 订单用户集合 key，例如：seckill:order:17
ARGV[1] - 用户 ID

返回值：
0 - 秒杀成功
1 - 库存不足
2 - 用户已抢过
]]

-- 1. 获取参数
local stockKey = KEYS[1]
local orderKey = KEYS[2]
local userId = ARGV[1]

-- 2. 查询库存（处理 key 不存在的情况）
local stock = tonumber(redis.call('get', stockKey))
if not stock or stock <= 0 then
    return 1
end

-- 3. 判断是否已下单
if redis.call('sismember', orderKey, userId) == 1 then
    return 2
end

-- 4. 扣减库存
redis.call('decr', stockKey)

-- 5. 记录用户
redis.call('sadd', orderKey, userId)

-- 6. 返回成功
return 0
