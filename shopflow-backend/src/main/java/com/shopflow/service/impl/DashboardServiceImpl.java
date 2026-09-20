package com.shopflow.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.constant.CacheKeys;
import com.shopflow.common.enums.OrderStatus;
import com.shopflow.mapper.DashboardMapper;
import com.shopflow.mapper.ProductMapper;
import com.shopflow.mapper.SysUserMapper;
import com.shopflow.service.DashboardService;
import com.shopflow.vo.dashboard.DashboardOverviewVO;
import com.shopflow.vo.dashboard.TopProductVO;
import com.shopflow.vo.dashboard.TrendPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板服务实现。
 *
 * <p>统计口径统一为「已支付及之后的订单」（status in 1,2,3），排除已取消订单，
 * 保证「订单数」「成交额」「热销榜」三者口径一致，不会出现对不上的情况。
 *
 * <p>概览指标每次都要 COUNT 多张表，因此加 60 秒缓存：
 * 后台看板允许秒级延迟，没必要每刷新一次就压一遍数据库。
 *
 * @author shopflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    /** 概览缓存有效期 */
    private static final Duration OVERVIEW_TTL = Duration.ofSeconds(60);

    /** 趋势最多查询天数 */
    private static final int MAX_TREND_DAYS = 30;

    /** 热销榜最多条数 */
    private static final int MAX_TOP_LIMIT = 20;

    private final DashboardMapper dashboardMapper;

    private final SysUserMapper userMapper;

    private final ProductMapper productMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public DashboardOverviewVO overview() {
        DashboardOverviewVO cached = readOverviewCache();
        if (cached != null) {
            return cached;
        }

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        long secondsSinceEpochToday = today.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault()).toInstant().getEpochSecond();
        // 累计成交额：从 Unix 纪元开始统计，等价于全量
        LocalDate epochDay = LocalDate.of(1970, 1, 1);

        DashboardOverviewVO overview = DashboardOverviewVO.builder()
                .userCount(defaultLong(userMapper.countValidUsers()))
                .productCount(defaultLong(productMapper.countValidProducts()))
                .orderCount(defaultLong(dashboardMapper.countValidOrders()))
                .pendingPaymentCount(defaultLong(
                        dashboardMapper.countOrdersByStatus(OrderStatus.PENDING_PAYMENT.getCode())))
                .todayOrderCount(defaultLong(dashboardMapper.countPaidOrders(
                        today.atStartOfDay(), tomorrow.atStartOfDay())))
                .todayAmount(defaultAmount(dashboardMapper.sumPaidAmount(
                        today.atStartOfDay(), tomorrow.atStartOfDay())))
                .totalAmount(defaultAmount(dashboardMapper.sumPaidAmount(
                        epochDay.atStartOfDay(), tomorrow.atStartOfDay())))
                .build();

        writeOverviewCache(overview);
        return overview;
    }

    @Override
    public List<TrendPointVO> trend(int days) {
        int safeDays = (days <= 0 || days > MAX_TREND_DAYS) ? 7 : days;
        LocalDate endDate = LocalDate.now().plusDays(1);
        LocalDate startDate = endDate.minusDays(safeDays);

        List<Map<String, Object>> rows = dashboardMapper.selectDailyTrend(
                startDate.atStartOfDay(), endDate.atStartOfDay());
        Map<String, TrendPointVO> pointMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String date = String.valueOf(row.get("d"));
            pointMap.put(date, new TrendPointVO(date,
                    toLong(row.get("orderCount")), toAmount(row.get("amount"))));
        }

        // 数据库只返回有订单的日期，这里补零保证前端折线图连续
        List<TrendPointVO> points = new ArrayList<>(safeDays);
        for (int i = 0; i < safeDays; i++) {
            String date = startDate.plusDays(i).toString();
            points.add(pointMap.getOrDefault(date, new TrendPointVO(date, 0L, BigDecimal.ZERO.setScale(2))));
        }
        return points;
    }

    @Override
    public List<TopProductVO> topProducts(int limit) {
        int safeLimit = (limit <= 0 || limit > MAX_TOP_LIMIT) ? 10 : limit;
        return dashboardMapper.selectTopProducts(safeLimit).stream().map(row -> {
            TopProductVO vo = new TopProductVO();
            vo.setProductId(toLong(row.get("productId")));
            vo.setProductName(row.get("productName") == null ? "" : String.valueOf(row.get("productName")));
            vo.setQuantity(toLong(row.get("quantity")));
            vo.setAmount(toAmount(row.get("amount")));
            return vo;
        }).toList();
    }

    // ==================== 内部方法 ====================

    private DashboardOverviewVO readOverviewCache() {
        try {
            String json = stringRedisTemplate.opsForValue().get(CacheKeys.DASHBOARD_OVERVIEW);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, DashboardOverviewVO.class);
        } catch (Exception ex) {
            log.warn("读取看板缓存失败，降级为实时统计 | error={}", ex.getMessage());
            return null;
        }
    }

    private void writeOverviewCache(DashboardOverviewVO overview) {
        try {
            stringRedisTemplate.opsForValue().set(CacheKeys.DASHBOARD_OVERVIEW,
                    objectMapper.writeValueAsString(overview), OVERVIEW_TTL);
        } catch (Exception ex) {
            log.warn("写入看板缓存失败 | error={}", ex.getMessage());
        }
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private BigDecimal toAmount(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return value instanceof Number number
                ? BigDecimal.valueOf(number.doubleValue()).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);
    }
}