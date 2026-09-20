package com.shopflow.common.util;

import com.shopflow.common.constant.CacheKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单号生成器。
 *
 * <p>格式：{@code SO + yyyyMMddHHmmss(14位) + 序列号(6位) + 随机数(4位)}，例如
 * {@code SO2026092015300000123456}，共 26 位。
 *
 * <p>设计考量：
 * <ul>
 *     <li>前缀带时间戳，便于肉眼定位、按时间范围排查，也天然具备趋势性；</li>
 *     <li>序列号用 Redis INCR 保证同一秒内递增且不重复；</li>
 *     <li>尾部随机数降低被猜测下单量、遍历订单号的风险；</li>
 *     <li>相比 UUID：长度更短、可读、作为唯一索引写入时更友好（避免随机 B+ 树分裂）。</li>
 * </ul>
 *
 * @author shopflow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNoGenerator {

    private static final String PREFIX = "SO";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 序列号取模上限，保证 6 位长度 */
    private static final long SEQUENCE_MOD = 1_000_000L;

    /** 序列 key 的有效期：同一秒内的 key 保留两分钟即可 */
    private static final Duration SEQUENCE_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate stringRedisTemplate;

    private final Random random = new Random();

    public String next() {
        String timePart = LocalDateTime.now().format(TIME_FORMATTER);
        long sequence = nextSequence(timePart);
        int tail = 1000 + random.nextInt(9000);
        return PREFIX + timePart + String.format("%06d", sequence % SEQUENCE_MOD) + tail;
    }

    /**
     * 取序列号。Redis 不可用时降级为随机序列，保证下单链路不因缓存故障而中断。
     */
    private long nextSequence(String timePart) {
        try {
            String key = CacheKeys.orderNoSeq(timePart);
            Long sequence = stringRedisTemplate.opsForValue().increment(key);
            if (sequence != null && sequence == 1L) {
                stringRedisTemplate.expire(key, SEQUENCE_TTL);
            }
            return sequence == null ? ThreadLocalRandom.current().nextLong(SEQUENCE_MOD) : sequence;
        } catch (Exception ex) {
            log.warn("订单号序列获取失败，降级为随机序列 | error={}", ex.getMessage());
            return ThreadLocalRandom.current().nextLong(SEQUENCE_MOD);
        }
    }
}