import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ShopFlow 轻量压测工具。
 *
 * <p>为什么自己写一个而不是用 JMeter/wrk：项目要在任何装了 JDK 17 的机器上"开箱可复现"，
 * 不引入额外安装步骤。基于 JDK 自带的 HttpClient 实现，逻辑透明、结果可核对。
 *
 * <p>用法：
 * <pre>
 * # 商品详情：固定并发 + 固定时长，用于对比"开缓存 vs 关缓存"
 * java LoadTest.java product &lt;baseUrl&gt; &lt;username&gt; &lt;password&gt; &lt;concurrency&gt; &lt;productId&gt; &lt;durationSeconds&gt;
 *
 * # 下单：固定并发 + 固定请求数，用于观察 QPS、成功率与库存不足的比例
 * java LoadTest.java order &lt;baseUrl&gt; &lt;username&gt; &lt;password&gt; &lt;concurrency&gt; &lt;totalRequests&gt; &lt;productId&gt; &lt;quantity&gt;
 * </pre>
 *
 * <p>指标口径：QPS = 总请求数 / 实际墙钟耗时；延迟为客户端观测值（含网络与服务端处理）。
 *
 * @author shopflow
 */
public class LoadTest {

    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CODE_PATTERN = Pattern.compile("\"code\"\\s*:\\s*(\\d+)");

    /** HTTP 响应摘要 */
    private record Result(int httpStatus, int code, String body) {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            printUsage();
            return;
        }
        String mode = args[0];
        String baseUrl = trimTrailingSlash(args[1]);
        String username = args[2];
        String password = args[3];
        int concurrency = Integer.parseInt(args[4]);

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        String token = login(client, baseUrl, username, password);
        System.out.printf("准备完成 | baseUrl=%s | user=%s | concurrency=%d%n", baseUrl, username, concurrency);

        if ("product".equals(mode)) {
            long productId = Long.parseLong(args[5]);
            int durationSeconds = Integer.parseInt(args[6]);
            runProductScenario(client, baseUrl, token, concurrency, productId, durationSeconds);
        } else if ("order".equals(mode)) {
            int totalRequests = Integer.parseInt(args[5]);
            long[] productIds = parseProductIds(args[6]);
            int quantity = Integer.parseInt(args[7]);
            runOrderScenario(client, baseUrl, token, concurrency, totalRequests, productIds, quantity);
        } else {
            printUsage();
        }
    }

    // ==================== 场景一：商品详情查询 ====================

    private static void runProductScenario(HttpClient client, String baseUrl, String token,
                                           int concurrency, long productId, int durationSeconds) throws Exception {
        String url = baseUrl + "/api/v1/products/" + productId;

        // 预热：把缓存、连接池与 JIT 都跑到稳定状态，避免把冷启动算进结果
        for (int i = 0; i < 300; i++) {
            send(client, url, token, null);
        }

        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(durationSeconds);
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startGate = new CountDownLatch(1);
        Map<Integer, AtomicInteger> codeCounter = new ConcurrentHashMap<>();

        List<Callable<List<Long>>> tasks = new ArrayList<>(concurrency);
        for (int i = 0; i < concurrency; i++) {
            tasks.add(() -> {
                List<Long> latencies = new ArrayList<>(200_000);
                startGate.await();
                while (System.nanoTime() < deadline) {
                    long begin = System.nanoTime();
                    Result result = send(client, url, token, null);
                    latencies.add(System.nanoTime() - begin);
                    codeCounter.computeIfAbsent(result.code(), k -> new AtomicInteger()).incrementAndGet();
                }
                return latencies;
            });
        }

        long wallBegin = System.nanoTime();
        startGate.countDown();
        List<Long> all = collect(pool, tasks);
        double wallSeconds = (System.nanoTime() - wallBegin) / 1e9;
        pool.shutdown();

        System.out.printf("场景=商品详情 duration=%ds productId=%d%n", durationSeconds, productId);
        printSummary(all, wallSeconds, codeCounter);
    }

    // ==================== 场景二：并发下单 ====================

    private static void runOrderScenario(HttpClient client, String baseUrl, String token, int concurrency,
                                         int totalRequests, long[] productIds, int quantity) throws Exception {
        String url = baseUrl + "/api/v1/orders";
        String runTag = "perf-" + System.currentTimeMillis();
        AtomicInteger cursor = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startGate = new CountDownLatch(1);
        Map<Integer, AtomicInteger> codeCounter = new ConcurrentHashMap<>();
        long expireTime = System.currentTimeMillis() + 3600_000L;

        List<Callable<List<Long>>> tasks = new ArrayList<>(concurrency);
        for (int i = 0; i < concurrency; i++) {
            tasks.add(() -> {
                List<Long> latencies = new ArrayList<>(4096);
                startGate.await();
                while (true) {
                    int index = cursor.getAndIncrement();
                    if (index >= totalRequests) {
                        break;
                    }
                    // 支持在多个商品之间打散流量：用于验证"单商品库存行锁竞争"对吞吐的影响
                    long productId = productIds[index % productIds.length];
                    String body = buildOrderBody(runTag + "-" + index, productId, quantity, expireTime);
                    long begin = System.nanoTime();
                    Result result = send(client, url, token, body);
                    latencies.add(System.nanoTime() - begin);
                    codeCounter.computeIfAbsent(result.code(), k -> new AtomicInteger()).incrementAndGet();
                }
                return latencies;
            });
        }

        long wallBegin = System.nanoTime();
        startGate.countDown();
        List<Long> all = collect(pool, tasks);
        double wallSeconds = (System.nanoTime() - wallBegin) / 1e9;
        pool.shutdown();

        System.out.printf("场景=并发下单 totalRequests=%d concurrency=%d products=%s quantity=%d runTag=%s%n",
                totalRequests, concurrency, java.util.Arrays.toString(productIds), quantity, runTag);
        printSummary(all, wallSeconds, codeCounter);
    }

    private static String buildOrderBody(String requestNo, long productId, int quantity, long expireTime) {
        return "{"
                + "\"requestNo\":\"" + requestNo + "\","
                + "\"receiverName\":\"压测用户\","
                + "\"receiverPhone\":\"13800000000\","
                + "\"receiverAddress\":\"广东省深圳市南山区压测地址\","
                + "\"remark\":\"load test\","
                + "\"items\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]"
                + "}";
    }

    // ==================== 公共方法 ====================

    private static List<Long> collect(ExecutorService pool, List<Callable<List<Long>>> tasks) throws Exception {
        List<Long> all = new ArrayList<>(1 << 20);
        for (Future<List<Long>> future : pool.invokeAll(tasks)) {
            all.addAll(future.get());
        }
        return all;
    }

    private static void printSummary(List<Long> latencies, double wallSeconds, Map<Integer, AtomicInteger> codeCounter) {
        int total = latencies.size();
        long success = codeCounter.getOrDefault(200, new AtomicInteger()).get();
        int errors = total - (int) success;

        latencies.sort(Long::compare);
        System.out.printf("总请求数=%d 成功=%d 失败=%d%n", total, success, errors);
        System.out.printf("墙钟耗时=%.2fs  QPS=%.1f%n", wallSeconds, wallSeconds > 0 ? total / wallSeconds : 0);
        System.out.printf("延迟(ms): 平均=%.1f P50=%.1f P95=%.1f P99=%.1f 最大=%.1f%n",
                average(latencies), percentile(latencies, 0.50), percentile(latencies, 0.95),
                percentile(latencies, 0.99), percentile(latencies, 1.00));
        System.out.print("业务码分布: ");
        codeCounter.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> System.out.printf("%d=%d ", entry.getKey(), entry.getValue().get()));
        System.out.println();
    }

    private static double average(List<Long> sortedNanos) {
        if (sortedNanos.isEmpty()) {
            return 0;
        }
        long sum = 0;
        for (long value : sortedNanos) {
            sum += value;
        }
        return sum / (double) sortedNanos.size() / 1_000_000;
    }

    private static double percentile(List<Long> sortedNanos, double percentile) {
        if (sortedNanos.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile * sortedNanos.size()) - 1;
        index = Math.max(0, Math.min(index, sortedNanos.size() - 1));
        return sortedNanos.get(index) / 1_000_000.0;
    }

    private static Result send(HttpClient client, String url, String token, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15));
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body == null) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        }
        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Result(response.statusCode(), parseCode(response.body()), response.body());
        } catch (Exception ex) {
            return new Result(-1, -1, ex.getClass().getSimpleName());
        }
    }

    private static int parseCode(String body) {
        if (body == null) {
            return -1;
        }
        Matcher matcher = CODE_PATTERN.matcher(body);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private static String login(HttpClient client, String baseUrl, String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Matcher matcher = ACCESS_TOKEN.matcher(response.body());
        if (!matcher.find()) {
            throw new IllegalStateException("登录失败，无法获取 accessToken：" + response.body());
        }
        return matcher.group(1);
    }

    /** 解析商品ID参数：支持 "1" 或 "2,3,4,5,6" 两种写法 */
    private static long[] parseProductIds(String spec) {
        String[] parts = spec.split(",");
        long[] ids = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ids[i] = Long.parseLong(parts[i].trim());
        }
        return ids;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static void printUsage() {
        System.out.println("""
                用法：
                  java LoadTest.java product <baseUrl> <username> <password> <concurrency> <productId> <durationSeconds>
                  java LoadTest.java order   <baseUrl> <username> <password> <concurrency> <totalRequests> <productId> <quantity>

                示例：
                  java LoadTest.java product http://localhost:8080 admin Admin@123456 50 1 20
                  java LoadTest.java order   http://localhost:8080 demo  User@123456   200 200 1 1
                """);
    }
}