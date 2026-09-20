package com.shopflow.task;

import com.shopflow.common.constant.CacheKeys;
import com.shopflow.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单超时关单任务。
 *
 * <p>实现方式：下单时把订单号写入 Redis ZSet（score = 支付截止时间戳），
 * 任务按分数区间取出已到期订单并逐笔关闭。
 *
 * <p>为什么不用「定时扫全表」？
 * <ul>
 *     <li>ZSet 只取真正到期的订单，避免每分钟全表扫描；</li>
 *     <li>即使 Redis 数据丢失，也可以通过 idx_status_expire_time 索引兜底扫表补数，
 *         属于「快路径 + 兜底路径」的组合；</li>
 *     <li>单笔关单是独立事务，一笔失败不影响其他订单。</li>
 * </ul>
 *
 * @author shopflow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    /** 单次最多处理多少笔，防止一次拉取过多导致长事务 */
    private static final int BATCH_SIZE = 100;

    private final OrderService orderService;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 每 60 秒扫描一次（启动后延迟 30 秒，先让应用预热完成）。
     */
    @Scheduled(initialDelayString = "${shopflow.order.timeout-scan-initial-delay-ms:30000}",
            fixedDelayString = "${shopflow.order.timeout-scan-interval-ms:60000}")
    public void closeTimeoutOrders() {
        List<String> timeoutOrderNos = orderService.findTimeoutOrderNos(BATCH_SIZE);
        if (timeoutOrderNos.isEmpty()) {
            return;
        }
        int closedCount = 0;
        for (String orderNo : timeoutOrderNos) {
            try {
                if (orderService.closeTimeoutOrder(orderNo)) {
                    closedCount++;
                }
            } catch (Exception ex) {
                log.error("关闭超时订单失败 | orderNo={}", orderNo, ex);
            } finally {
                // 无论成功失败都出队，避免同一条数据被反复处理
                stringRedisTemplate.opsForZSet().remove(CacheKeys.ORDER_TIMEOUT_ZSET, orderNo);
            }
        }
        if (closedCount > 0) {
            log.info("超时关单任务完成 | scanned={} | closed={}", timeoutOrderNos.size(), closedCount);
        }
    }
}