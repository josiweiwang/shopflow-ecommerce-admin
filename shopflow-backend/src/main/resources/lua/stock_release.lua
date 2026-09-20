-- 库存释放脚本（取消订单、超时关单时使用）
-- KEYS[1] 商品可用库存 key
-- ARGV[1] 释放数量
-- 返回值：1-释放成功  -1-缓存不存在（由业务层决定是否回源）
local stock = redis.call('GET', KEYS[1])
if not stock then
    return -1
end
redis.call('INCRBY', KEYS[1], ARGV[1])
return 1