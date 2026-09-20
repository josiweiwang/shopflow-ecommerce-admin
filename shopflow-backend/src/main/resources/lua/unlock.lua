-- 分布式锁释放脚本（Compare And Delete）
-- 只有锁的值等于自己持有的 token 时才删除，避免把别人的锁误删
-- KEYS[1] 锁 key  ARGV[1] 自己持有的 token
-- 返回值：1-删除成功  0-锁不属于自己（可能已超时自动释放）
if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0