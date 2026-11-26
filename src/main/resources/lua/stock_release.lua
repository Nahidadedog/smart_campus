-- 释放预占：只有该学生确实在集合里才加回名额，天然幂等（重复调用不会多加）
-- KEYS[1] = 库存 key, KEYS[2] = 已选学生集合 key, ARGV[1] = studentId
-- 返回：1 释放成功 / 0 该学生本来就不在集合中
local removed = redis.call('SREM', KEYS[2], ARGV[1])
if removed == 1 and redis.call('EXISTS', KEYS[1]) == 1 then
    redis.call('INCR', KEYS[1])
end
return removed
