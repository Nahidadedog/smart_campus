-- 令牌桶限流（按 key 隔离，例如每个用户一个桶）
-- KEYS[1] = 桶 key（Hash: tokens 当前令牌数, ts 上次补充时间戳毫秒）
-- ARGV[1] = capacity 桶容量, ARGV[2] = refillPerSecond 每秒补充速率, ARGV[3] = requested 本次消耗
-- 时间取自 Redis 自身的 TIME，多实例之间不会有应用时钟偏差
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill = tonumber(ARGV[2])
local requested = tonumber(ARGV[3])

local t = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

local bucket = redis.call('HMGET', key, 'tokens', 'ts')
local tokens = tonumber(bucket[1])
local ts = tonumber(bucket[2])
if tokens == nil then
    tokens = capacity
    ts = now
end

local elapsed = math.max(0, now - ts) / 1000.0
tokens = math.min(capacity, tokens + elapsed * refill)

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

redis.call('HSET', key, 'tokens', tokens, 'ts', now)
-- 空桶攒满所需时间 + 1 秒冗余，过期自动清理
redis.call('PEXPIRE', key, math.ceil(capacity / refill * 1000) + 1000)
return allowed
