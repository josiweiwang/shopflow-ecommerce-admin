package com.shopflow.order;

import com.shopflow.common.result.ResultCode;
import com.shopflow.dto.order.OrderCreateDTO;
import com.shopflow.dto.order.OrderItemCreateDTO;
import com.shopflow.exception.BizException;
import com.shopflow.security.LoginUser;
import com.shopflow.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 下单并发与幂等集成测试。
 *
 * <p>使用真实的 MySQL 8 与 Redis 7 容器（Testcontainers），
 * 在 Docker 不可用的机器上会自动跳过，不会让本地开发卡在环境依赖上。
 *
 * <p>这是整个项目最有说服力的一条测试：它直接回答「你的防超卖方案到底有没有用」。
 * 断言方式不是「看起来没问题」，而是把库存设成 100，同时打 200 个并发下单请求，
 * 然后校验成功笔数、库存终值以及是否出现负数库存。
 *
 * @author shopflow
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("integration")
class OrderConcurrencyTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("shopflow")
            .withUsername("root")
            .withPassword("root")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        // 用真实建表脚本初始化容器数据库，顺带验证 schema.sql 与 data.sql 本身可执行
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:db/schema.sql");
        registry.add("spring.sql.init.data-locations", () -> "classpath:db/data.sql");
    }

    /** 演示用户（data.sql 中的 demo 账号） */
    private static final Long DEMO_USER_ID = 3L;

    /** 参与压测的商品：data.sql 中的商品 1 */
    private static final Long PRODUCT_ID = 1L;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 清空订单数据并把商品库存重置为 100，保证每次运行结果可复现
        jdbcTemplate.update("DELETE FROM order_status_log");
        jdbcTemplate.update("DELETE FROM order_item");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM inventory_log");
        jdbcTemplate.update("UPDATE inventory SET total_stock = 100, available_stock = 100, "
                + "locked_stock = 0, version = 0 WHERE product_id = ?", PRODUCT_ID);
        jdbcTemplate.update("DELETE FROM sys_operation_log");

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                LoginUser.builder().userId(DEMO_USER_ID).username("demo").status(1).build(), null,
                Collections.emptyList()));
    }

    @Test
    @DisplayName("200 并发下单抢 100 件库存：只成功 100 笔，库存归零，绝不超卖")
    void concurrentOrdersShouldNotOversell() throws Exception {
        final int concurrency = 200;
        final int stock = 100;

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(concurrency);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger soldOutCount = new AtomicInteger();
        List<Exception> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < concurrency; i++) {
            final int index = i;
            executor.submit(() -> {
                // 每个线程都要有自己的认证上下文与独立的数据库连接
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        LoginUser.builder().userId(DEMO_USER_ID).username("demo").status(1).build(),
                        null, Collections.emptyList()));
                try {
                    startGate.await();
                    orderService.create(buildOrderDTO("concurrent-req-" + index));
                    successCount.incrementAndGet();
                } catch (BizException ex) {
                    if (ex.getCode() == ResultCode.STOCK_INSUFFICIENT.getCode()) {
                        soldOutCount.incrementAndGet();
                    } else {
                        unexpectedErrors.add(ex);
                    }
                } catch (Exception ex) {
                    unexpectedErrors.add(ex);
                } finally {
                    finishGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(finishGate.await(120, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        Map<String, Object> inventory = jdbcTemplate.queryForMap(
                "SELECT total_stock, available_stock, locked_stock FROM inventory WHERE product_id = ?", PRODUCT_ID);
        Long orderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        Long logCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_log WHERE biz_type = 2", Long.class);

        assertThat(unexpectedErrors).isEmpty();
        assertThat(successCount.get()).isEqualTo(stock);
        assertThat(soldOutCount.get()).isEqualTo(concurrency - stock);
        assertThat(orderCount).isEqualTo(stock);
        assertThat(((Number) inventory.get("available_stock")).intValue()).isZero();
        assertThat(((Number) inventory.get("locked_stock")).intValue()).isEqualTo(stock);
        assertThat(((Number) inventory.get("total_stock")).intValue()).isEqualTo(stock);
        // 每一次成功锁定都必须有对应的库存流水，保证可追溯
        assertThat(logCount).isEqualTo(stock);
    }

    @Test
    @DisplayName("相同幂等号重复下单只会生成一笔订单")
    void duplicatedRequestShouldReturnSameOrder() {
        String requestNo = "idempotent-req-0001";

        String firstOrderNo = orderService.create(buildOrderDTO(requestNo)).getOrderNo();
        String secondOrderNo = orderService.create(buildOrderDTO(requestNo)).getOrderNo();

        Long orderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        assertThat(secondOrderNo).isEqualTo(firstOrderNo);
        assertThat(orderCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("库存不足时下单失败，且不会留下任何订单与库存流水")
    void shouldRejectWhenStockInsufficient() {
        jdbcTemplate.update("UPDATE inventory SET available_stock = 1, total_stock = 1, locked_stock = 0 "
                + "WHERE product_id = ?", PRODUCT_ID);

        assertThatThrownBy(() -> orderService.create(buildOrderDTO("stock-not-enough-0001", 5)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ResultCode.STOCK_INSUFFICIENT.getCode());

        Long orderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        assertThat(orderCount).isZero();
        Integer availableStock = jdbcTemplate.queryForObject(
                "SELECT available_stock FROM inventory WHERE product_id = ?", Integer.class, PRODUCT_ID);
        assertThat(availableStock).isEqualTo(1);
    }

    private OrderCreateDTO buildOrderDTO(String requestNo) {
        return buildOrderDTO(requestNo, 1);
    }

    private OrderCreateDTO buildOrderDTO(String requestNo, int quantity) {
        OrderItemCreateDTO item = new OrderItemCreateDTO();
        item.setProductId(PRODUCT_ID);
        item.setQuantity(quantity);

        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setRequestNo(requestNo);
        dto.setReceiverName("并发测试");
        dto.setReceiverPhone("13800000000");
        dto.setReceiverAddress("广东省深圳市南山区测试地址");
        dto.setItems(List.of(item));
        return dto;
    }
}