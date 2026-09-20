package com.shopflow.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置。
 *
 * <p>序列化策略：
 * <ul>
 *     <li>Key 用 String 序列化，保证 redis-cli 里可读，便于排查问题；</li>
 *     <li>Value 用 JSON 序列化（带类型信息），既能存对象又不会被 JDK 序列化绑死。</li>
 * </ul>
 *
 * <p>注意：库存扣减、分布式锁等原子操作统一使用 StringRedisTemplate + Lua 脚本，
 * 不走这里的 JSON 序列化，避免序列化开销与类型转换问题。
 *
 * @author shopflow
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(buildObjectMapper());

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 库存预扣脚本：把「读库存 -> 判断 -> 扣减」放在 Redis 内原子执行。
     *
     * <p>如果拆成多条命令，两个并发请求可能同时读到「还剩 1 件」而各自扣减成功，
     * 造成超卖。Lua 脚本在 Redis 中单线程原子执行，从根本上消除这个竞态。
     */
    @Bean
    public RedisScript<Long> stockLockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/stock_lock.lua"));
        script.setResultType(Long.class);
        return script;
    }

    /** 库存释放脚本 */
    @Bean
    public RedisScript<Long> stockReleaseScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/stock_release.lua"));
        script.setResultType(Long.class);
        return script;
    }

    /** 分布式锁释放脚本：比较 token 后再删除，防止误删他人锁 */
    @Bean
    public RedisScript<Long> unlockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/unlock.lua"));
        script.setResultType(Long.class);
        return script;
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        return objectMapper;
    }
}