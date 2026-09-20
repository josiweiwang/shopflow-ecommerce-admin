package com.shopflow.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.constant.CacheKeys;
import com.shopflow.entity.SysUser;
import com.shopflow.mapper.SysPermissionMapper;
import com.shopflow.mapper.SysRoleMapper;
import com.shopflow.mapper.SysUserMapper;
import com.shopflow.security.LoginUser;
import com.shopflow.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashSet;

/**
 * 登录用户与权限加载实现。
 *
 * @author shopflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {

    /** 权限缓存有效期：权衡「权限变更生效速度」与「数据库压力」 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    private final SysPermissionMapper permissionMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public LoginUser loadLoginUser(Long userId) {
        if (userId == null) {
            return null;
        }
        String cacheKey = CacheKeys.authLoginUser(userId);
        String cachedJson = readFromCache(cacheKey);
        if (cachedJson != null) {
            try {
                return objectMapper.readValue(cachedJson, LoginUser.class);
            } catch (Exception ex) {
                log.warn("权限缓存反序列化失败，降级为查询数据库 | key={} | error={}", cacheKey, ex.getMessage());
            }
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .roleCodes(new HashSet<>(roleMapper.selectRoleCodesByUserId(userId)))
                .permissions(new HashSet<>(permissionMapper.selectPermissionCodesByUserId(userId)))
                .build();

        writeToCache(cacheKey, loginUser);
        return loginUser;
    }

    @Override
    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(CacheKeys.authLoginUser(userId));
        log.info("已清除用户权限缓存 | userId={}", userId);
    }

    /**
     * 读缓存。Redis 异常时降级为直接查库：缓存不可用不应导致整个系统不可用。
     */
    private String readFromCache(String cacheKey) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (!StringUtils.hasText(cached)) {
                return null;
            }
            return cached;
        } catch (Exception ex) {
            log.warn("读取权限缓存失败，降级为查询数据库 | key={} | error={}", cacheKey, ex.getMessage());
            return null;
        }
    }

    private void writeToCache(String cacheKey, LoginUser loginUser) {
        try {
            stringRedisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(loginUser), CACHE_TTL);
        } catch (Exception ex) {
            log.warn("写入权限缓存失败，不影响本次请求 | key={} | error={}", cacheKey, ex.getMessage());
        }
    }
}