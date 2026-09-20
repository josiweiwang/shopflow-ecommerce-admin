package com.shopflow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopflow.common.constant.CacheKeys;
import com.shopflow.common.enums.OrderOperatorType;
import com.shopflow.common.enums.OrderStatus;
import com.shopflow.common.enums.ProductStatus;
import com.shopflow.common.result.PageResult;
import com.shopflow.common.result.ResultCode;
import com.shopflow.common.util.OrderNoGenerator;
import com.shopflow.common.util.OrderStatusMachine;
import com.shopflow.common.util.TransactionUtils;
import com.shopflow.convert.OrderConverter;
import com.shopflow.dto.order.OrderCancelDTO;
import com.shopflow.dto.order.OrderCreateDTO;
import com.shopflow.dto.order.OrderItemCreateDTO;
import com.shopflow.dto.order.OrderQueryDTO;
import com.shopflow.dto.order.OrderStatusUpdateDTO;
import com.shopflow.entity.OrderItem;
import com.shopflow.entity.OrderStatusLog;
import com.shopflow.entity.Orders;
import com.shopflow.entity.Product;
import com.shopflow.exception.BizException;
import com.shopflow.mapper.OrderItemMapper;
import com.shopflow.mapper.OrderStatusLogMapper;
import com.shopflow.mapper.OrdersMapper;
import com.shopflow.mapper.ProductMapper;
import com.shopflow.security.LoginUser;
import com.shopflow.security.SecurityUtils;
import com.shopflow.service.InventoryService;
import com.shopflow.service.OrderService;
import com.shopflow.vo.order.OrderCreateVO;
import com.shopflow.vo.order.OrderDetailVO;
import com.shopflow.vo.order.OrderItemVO;
import com.shopflow.vo.order.OrderPageVO;
import com.shopflow.vo.order.OrderStatusLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 订单服务实现。
 *
 * <p>下单主流程（全部在一个事务内，保证「订单、明细、库存、流水」四者要么全成功要么全失败）：
 * <ol>
 *     <li>幂等校验：同一用户 + 同一 requestNo 只会生成一笔订单；</li>
 *     <li>商品校验：商品必须存在且处于上架状态，价格一律以数据库为准，绝不信任前端传参；</li>
 *     <li>库存预扣：Redis Lua 原子扣减 + MySQL 条件更新双保险；</li>
 *     <li>写入订单、明细、状态流水；</li>
 *     <li>订单号写入 Redis ZSet 延时队列，用于超时自动关单。</li>
 * </ol>
 *
 * <p>事务回滚时会同时把 Redis 中已预扣的库存加回去，
 * 避免出现「数据库没扣、缓存却少了」的幽灵库存。
 *
 * @author shopflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrdersMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final OrderStatusLogMapper orderStatusLogMapper;

    private final ProductMapper productMapper;

    private final InventoryService inventoryService;

    private final OrderConverter orderConverter;

    private final OrderNoGenerator orderNoGenerator;

    private final OrderStatusMachine orderStatusMachine;

    private final StringRedisTemplate stringRedisTemplate;

    /** 支付超时时间（分钟），超时后由定时任务自动关单并释放库存 */
    @Value("${shopflow.order.pay-timeout-minutes:30}")
    private int payTimeoutMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO create(OrderCreateDTO dto) {
        LoginUser loginUser = requireLogin();
        Long userId = loginUser.getUserId();

        // ---------- 1. 幂等校验 ----------
        Orders duplicated = findByIdempotentKey(userId, dto.getRequestNo());
        if (duplicated != null) {
            return buildCreateVO(duplicated);
        }
        String idempotentKey = CacheKeys.orderIdempotent(userId, dto.getRequestNo());
        Boolean firstRequest = trySetIdempotentKey(idempotentKey);
        if (Boolean.FALSE.equals(firstRequest)) {
            Orders concurrent = findByIdempotentKey(userId, dto.getRequestNo());
            if (concurrent != null) {
                return buildCreateVO(concurrent);
            }
            throw new BizException(ResultCode.DUPLICATE_REQUEST);
        }

        // ---------- 2. 商品与金额校验 ----------
        Map<Long, Integer> quantityMap = mergeItems(dto.getItems());
        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        String orderNo = orderNoGenerator.next();

        for (Map.Entry<Long, Integer> entry : quantityMap.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new BizException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在或已删除，productId=" + productId);
            }
            if (ProductStatus.fromCode(product.getStatus()) != ProductStatus.ON_SHELF) {
                throw new BizException(ResultCode.PRODUCT_OFF_SHELF, "商品【" + product.getName() + "】已下架");
            }
            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem();
            item.setOrderNo(orderNo);
            item.setProductId(product.getId());
            item.setSku(product.getSku());
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage() == null ? "" : product.getMainImage());
            item.setPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setSubtotal(subtotal);
            items.add(item);
        }
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        // ---------- 3. 库存预扣（失败则整单失败）----------
        List<OrderItem> lockedItems = new ArrayList<>();
        registerCacheRollbackHook(lockedItems);
        for (OrderItem item : items) {
            boolean locked = inventoryService.lockStock(item.getProductId(), item.getQuantity(), orderNo);
            if (!locked) {
                throw new BizException(ResultCode.STOCK_INSUFFICIENT,
                        "商品【" + item.getProductName() + "】库存不足");
            }
            lockedItems.add(item);
        }

        // ---------- 4. 写入订单与明细 ----------
        LocalDateTime now = LocalDateTime.now();
        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setFreightAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setPayAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        order.setRequestNo(dto.getRequestNo());
        order.setExpireTime(now.plusMinutes(payTimeoutMinutes));
        order.setCancelReason("");
        orderMapper.insert(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        writeStatusLog(order, null, OrderStatus.PENDING_PAYMENT, OrderOperatorType.USER, "创建订单");

        // ---------- 5. 写入延时队列（事务提交后）----------
        LocalDateTime expireTime = order.getExpireTime();
        TransactionUtils.afterCommit(() -> addTimeoutQueue(orderNo, expireTime));

        log.info("创建订单成功 | orderNo={} | userId={} | payAmount={} | itemCount={}",
                orderNo, userId, totalAmount, items.size());
        return buildCreateVO(order);
    }

    @Override
    public OrderDetailVO detail(String orderNo) {
        Orders order = getExisting(orderNo);
        assertCanAccess(order);

        OrderDetailVO detail = orderConverter.toDetailVO(order);
        List<OrderItemVO> items = orderItemMapper.selectByOrderNo(orderNo).stream()
                .map(orderConverter::toItemVO)
                .toList();
        List<OrderStatusLogVO> logs = orderStatusLogMapper.selectList(
                        Wrappers.lambdaQuery(OrderStatusLog.class)
                                .eq(OrderStatusLog::getOrderNo, orderNo)
                                .orderByAsc(OrderStatusLog::getId)).stream()
                .map(orderConverter::toStatusLogVO)
                .toList();
        detail.setItems(items);
        detail.setStatusLogs(logs);
        detail.setItemCount(items.size());
        detail.setTotalQuantity(items.stream().mapToInt(OrderItemVO::getQuantity).sum());
        return detail;
    }

    @Override
    public PageResult<OrderPageVO> page(OrderQueryDTO query) {
        LoginUser loginUser = requireLogin();
        if (!loginUser.isAdmin()) {
            // 前台用户只能查询自己的订单：由服务端强制覆盖条件，前端无法越权
            query.setUserId(loginUser.getUserId());
        }
        Page<OrderPageVO> page = new Page<>(query.safePageNum(), query.safePageSize());
        IPage<OrderPageVO> result = orderMapper.selectPageByCondition(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(String orderNo) {
        Orders order = getExisting(orderNo);
        assertCanAccess(order);

        OrderStatus from = OrderStatus.requireFromCode(order.getStatus());
        if (from != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL,
                    "订单当前状态为【" + from.getDescription() + "】，无需重复支付");
        }
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            // 已超时：本次支付直接拒绝，定时任务会在一分钟内关单并释放库存
            throw new BizException(ResultCode.ORDER_PAY_TIMEOUT);
        }

        boolean updated = updateStatusWithCondition(order.getId(), from, OrderStatus.PAID,
                LocalDateTime.now(), null, null, null, null);
        if (!updated) {
            throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL, "订单状态已变更，请刷新后重试");
        }

        // 支付成功：锁定库存转为实际扣减，并累加商品销量
        for (OrderItem item : orderItemMapper.selectByOrderNo(orderNo)) {
            inventoryService.confirmDeduct(item.getProductId(), item.getQuantity(), orderNo);
            productMapper.increaseSales(item.getProductId(), item.getQuantity());
        }
        writeStatusLog(order, from, OrderStatus.PAID, resolveOperatorType(), "支付成功");
        removeTimeoutQueue(orderNo);
        log.info("订单支付成功 | orderNo={} | payAmount={}", orderNo, order.getPayAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo, OrderCancelDTO dto) {
        Orders order = getExisting(orderNo);
        assertCanAccess(order);
        OrderStatus from = OrderStatus.requireFromCode(order.getStatus());
        // 只有待支付订单可以取消：已支付订单涉及退款流程，本项目不做真实支付因此直接拒绝
        orderStatusMachine.assertAllowed(from, OrderStatus.CANCELED);

        String reason = (dto == null || dto.getReason() == null || dto.getReason().isBlank())
                ? "用户主动取消" : dto.getReason();
        doCancel(order, from, reason, resolveOperatorType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String orderNo, OrderStatusUpdateDTO dto) {
        Orders order = getExisting(orderNo);
        OrderStatus from = OrderStatus.requireFromCode(order.getStatus());
        OrderStatus to = OrderStatus.requireFromCode(dto.getTargetStatus());
        orderStatusMachine.assertAllowed(from, to);

        LocalDateTime now = LocalDateTime.now();
        boolean updated = switch (to) {
            case DELIVERING -> updateStatusWithCondition(order.getId(), from, to, null, now, null, null, null);
            case COMPLETED -> updateStatusWithCondition(order.getId(), from, to, null, null, now, null, null);
            default -> throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL,
                    "不支持通过该接口流转到【" + to.getDescription() + "】");
        };
        if (!updated) {
            throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL, "订单状态已变更，请刷新后重试");
        }
        String remark = (dto.getRemark() == null || dto.getRemark().isBlank())
                ? "订单状态变更为【" + to.getDescription() + "】" : dto.getRemark();
        writeStatusLog(order, from, to, OrderOperatorType.ADMIN, remark);
        log.info("后台变更订单状态 | orderNo={} | {} -> {} | operatorId={}",
                orderNo, from.getDescription(), to.getDescription(), SecurityUtils.getUserIdOrZero());
    }

    @Override
    public List<String> findTimeoutOrderNos(int batchSize) {
        try {
            Set<String> orderNos = stringRedisTemplate.opsForZSet().rangeByScore(
                    CacheKeys.ORDER_TIMEOUT_ZSET, 0, System.currentTimeMillis(), 0, batchSize);
            return orderNos == null ? List.of() : new ArrayList<>(orderNos);
        } catch (Exception ex) {
            log.warn("读取订单超时队列失败 | error={}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeTimeoutOrder(String orderNo) {
        Orders order = orderMapper.selectOne(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            return false;
        }
        OrderStatus from = OrderStatus.fromCode(order.getStatus());
        if (from == null || !from.isPendingPayment()) {
            // 已支付或已取消，无需处理
            return false;
        }
        if (order.getExpireTime() != null && order.getExpireTime().isAfter(LocalDateTime.now())) {
            return false;
        }
        doCancel(order, from, "超过支付时限，系统自动取消", OrderOperatorType.SYSTEM);
        log.info("超时订单已自动关闭 | orderNo={}", orderNo);
        return true;
    }

    // ==================== 内部方法 ====================

    /**
     * 执行取消：状态流转 + 释放库存 + 写流水 + 移出延时队列。
     */
    private void doCancel(Orders order, OrderStatus from, String reason, OrderOperatorType operatorType) {
        boolean updated = updateStatusWithCondition(order.getId(), from, OrderStatus.CANCELED,
                null, null, null, LocalDateTime.now(), reason);
        if (!updated) {
            throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL, "订单状态已变更，请刷新后重试");
        }
        for (OrderItem item : orderItemMapper.selectByOrderNo(order.getOrderNo())) {
            inventoryService.releaseStock(item.getProductId(), item.getQuantity(),
                    order.getOrderNo(), "订单取消，释放锁定库存");
        }
        writeStatusLog(order, from, OrderStatus.CANCELED, operatorType, reason);
        removeTimeoutQueue(order.getOrderNo());
    }

    /**
     * 条件更新：把「原状态」写进 WHERE，防止并发下用旧状态覆盖新状态。
     */
    private boolean updateStatusWithCondition(Long orderId, OrderStatus from, OrderStatus to,
                                              LocalDateTime payTime, LocalDateTime deliverTime,
                                              LocalDateTime finishTime, LocalDateTime cancelTime,
                                              String cancelReason) {
        Orders update = new Orders();
        update.setStatus(to.getCode());
        if (payTime != null) {
            update.setPayTime(payTime);
        }
        if (deliverTime != null) {
            update.setDeliverTime(deliverTime);
        }
        if (finishTime != null) {
            update.setFinishTime(finishTime);
        }
        if (cancelTime != null) {
            update.setCancelTime(cancelTime);
        }
        if (cancelReason != null) {
            update.setCancelReason(cancelReason);
        }
        return orderMapper.update(update, Wrappers.lambdaUpdate(Orders.class)
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, from.getCode())) > 0;
    }

    private void writeStatusLog(Orders order, OrderStatus from, OrderStatus to,
                                OrderOperatorType operatorType, String remark) {
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(order.getId());
        statusLog.setOrderNo(order.getOrderNo());
        statusLog.setFromStatus(from == null ? null : from.getCode());
        statusLog.setToStatus(to.getCode());
        statusLog.setOperatorId(SecurityUtils.getUserIdOrZero());
        statusLog.setOperatorType(operatorType.getCode());
        statusLog.setRemark(remark == null ? "" : remark);
        orderStatusLogMapper.insert(statusLog);
    }

    /**
     * 合并重复商品（同一商品提交多次时数量相加），并保持提交顺序。
     */
    private Map<Long, Integer> mergeItems(List<OrderItemCreateDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(ResultCode.ORDER_ITEM_EMPTY);
        }
        Map<Long, Integer> quantityMap = new LinkedHashMap<>();
        for (OrderItemCreateDTO item : items) {
            quantityMap.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantityMap;
    }

    /**
     * 尝试写入幂等键。
     *
     * @return TRUE 首次请求；FALSE 已存在（重复提交）；NULL Redis 异常，降级为数据库唯一索引兜底
     */
    private Boolean trySetIdempotentKey(String idempotentKey) {
        try {
            return stringRedisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", Duration.ofHours(24));
        } catch (Exception ex) {
            log.warn("幂等键写入失败，降级为数据库唯一索引兜底 | key={} | error={}", idempotentKey, ex.getMessage());
            return null;
        }
    }

    private Orders findByIdempotentKey(Long userId, String requestNo) {
        return orderMapper.selectOne(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getUserId, userId)
                .eq(Orders::getRequestNo, requestNo)
                .last("LIMIT 1"));
    }

    private Orders getExisting(String orderNo) {
        Orders order = orderMapper.selectOne(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 越权防护：前台用户只能操作自己的订单，管理员不受限。
     */
    private void assertCanAccess(Orders order) {
        LoginUser loginUser = requireLogin();
        if (loginUser.isAdmin()) {
            return;
        }
        if (!loginUser.getUserId().equals(order.getUserId())) {
            log.warn("越权访问订单 | userId={} | orderOwnerId={} | orderNo={}",
                    loginUser.getUserId(), order.getUserId(), order.getOrderNo());
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该订单");
        }
    }

    private LoginUser requireLogin() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    private OrderOperatorType resolveOperatorType() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return (loginUser != null && loginUser.isAdmin()) ? OrderOperatorType.ADMIN : OrderOperatorType.USER;
    }

    private OrderCreateVO buildCreateVO(Orders order) {
        return OrderCreateVO.builder()
                .orderNo(order.getOrderNo())
                .payAmount(order.getPayAmount())
                .status(order.getStatus())
                .expireTime(order.getExpireTime())
                .build();
    }

    private void addTimeoutQueue(String orderNo, LocalDateTime expireTime) {
        try {
            long score = expireTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            stringRedisTemplate.opsForZSet().add(CacheKeys.ORDER_TIMEOUT_ZSET, orderNo, score);
        } catch (Exception ex) {
            log.warn("订单写入超时队列失败，将由数据库兜底扫描 | orderNo={} | error={}", orderNo, ex.getMessage());
        }
    }

    private void removeTimeoutQueue(String orderNo) {
        try {
            stringRedisTemplate.opsForZSet().remove(CacheKeys.ORDER_TIMEOUT_ZSET, orderNo);
        } catch (Exception ex) {
            log.warn("订单移除超时队列失败 | orderNo={} | error={}", orderNo, ex.getMessage());
        }
    }

    /**
     * 注册事务回滚钩子：数据库回滚时把 Redis 已预扣的库存加回去。
     *
     * <p>这是「缓存 + 数据库」双写场景里最容易漏掉的一环：
     * 事务失败后数据库库存恢复了，但 Redis 里少掉的那部分库存如果不还回去，
     * 就会变成永远卖不掉的幽灵库存。
     */
    private void registerCacheRollbackHook(List<OrderItem> lockedItems) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_ROLLED_BACK) {
                    return;
                }
                for (OrderItem item : lockedItems) {
                    inventoryService.releaseStock(item.getProductId(), item.getQuantity(),
                            item.getOrderNo(), "下单事务回滚，回补缓存库存");
                }
                log.warn("下单事务回滚，已回补缓存库存 | itemCount={}", lockedItems.size());
            }
        });
    }
}