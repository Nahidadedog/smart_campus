-- 选课名额原子预扣（Redis 单线程执行脚本，判断与扣减之间不会插入其它命令）
-- KEYS[1] = 库存 key（String，剩余名额）
-- KEYS[2] = 已选学生集合 key（Set，studentId）
-- ARGV[1] = studentId
-- 返回：1 占用成功 / 0 名额已满 / -1 该学生已选或正在处理 / -2 缓存未加载（调用方需从数据库预热后重试）
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -1
end
if redis.call('EXISTS', KEYS[1]) == 0 then
    return -2
end
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil or stock <= 0 then
    return 0
end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1
