-- 以数据库为准重建某门课的名额缓存（SET 与集合重建在同一脚本内，期间不会插入其它命令）
-- KEYS[1] = 库存 key, KEYS[2] = 已选学生集合 key
-- ARGV[1] = 剩余名额, ARGV[2..] = 已选 studentId 列表
redis.call('SET', KEYS[1], ARGV[1])
redis.call('DEL', KEYS[2])
for i = 2, #ARGV do
    redis.call('SADD', KEYS[2], ARGV[i])
end
return 1
