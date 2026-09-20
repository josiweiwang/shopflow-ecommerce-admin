-- 库存预扣脚本（原子执行，下单防超卖的第一道防线）
-- KEYS[1] 商品可用库存 key，如 shopflow:inventory:stock:1001
-- ARGV[1] 本次需要锁定的数量
-- 返回值：1-扣减成功  0-库存不足  -1-缓存不存在（需要回源数据库预热）
local stock = redis.call('GET', KEYS[1])
if not stock then
    return -1
end
if tonumber(stock) < tonumber(ARGV[1]) then
    return 0
end
redis.call('DECRBY', KEYS[1], ARGV[1])
return 1