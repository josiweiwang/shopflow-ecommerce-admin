package com.shopflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopflow.common.constant.CacheKeys;
import com.shopflow.common.enums.RoleCode;
import com.shopflow.common.enums.UserStatus;
import com.shopflow.common.result.ResultCode;
import com.shopflow.config.JwtProperties;
import com.shopflow.convert.UserConverter;
import com.shopflow.dto.auth.ChangePasswordDTO;
import com.shopflow.dto.auth.LoginDTO;
import com.shopflow.dto.auth.RegisterDTO;
import com.shopflow.dto.auth.UpdateProfileDTO;
import com.shopflow.entity.SysRole;
import com.shopflow.entity.SysUser;
import com.shopflow.entity.SysUserRole;
import com.shopflow.exception.BizException;
import com.shopflow.mapper.SysRoleMapper;
import com.shopflow.mapper.SysUserMapper;
import com.shopflow.mapper.SysUserRoleMapper;
import com.shopflow.security.JwtTokenProvider;
import com.shopflow.security.LoginUser;
import com.shopflow.security.SecurityUtils;
import com.shopflow.service.AuthService;
import com.shopflow.service.UserPermissionService;
import com.shopflow.vo.auth.LoginVO;
import com.shopflow.vo.auth.UserInfoVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 认证服务实现。
 *
 * @author shopflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final JwtProperties jwtProperties;

    private final UserPermissionService userPermissionService;

    private final StringRedisTemplate stringRedisTemplate;

    private final UserConverter userConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // 先做友好校验，真正的唯一性由数据库唯一索引兜底（并发注册时抛 DuplicateKeyException -> 409）
        assertUsernameAvailable(dto.getUsername());
        assertEmailAvailable(dto.getEmail(), null);
        assertPhoneAvailable(dto.getPhone(), null);

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setEmail(normalize(dto.getEmail()));
        user.setPhone(normalize(dto.getPhone()));
        user.setAvatar("");
        user.setStatus(UserStatus.ENABLED.getCode());
        user.setLastLoginIp("");
        userMapper.insert(user);

        SysRole defaultRole = roleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class)
                .eq(SysRole::getRoleCode, RoleCode.USER)
                .eq(SysRole::getStatus, UserStatus.ENABLED.getCode())
                .last("LIMIT 1"));
        if (defaultRole == null) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "默认角色未初始化，请联系管理员");
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(defaultRole.getId());
        userRoleMapper.insert(userRole);

        log.info("用户注册成功 | userId={} | username={}", user.getId(), user.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO dto, String clientIp) {
        SysUser user = userMapper.selectOne(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, dto.getUsername())
                .last("LIMIT 1"));
        // 用户不存在与密码错误返回同一个提示，避免被用来枚举系统内有哪些账号
        if (user == null) {
            log.warn("登录失败：用户不存在 | username={}", dto.getUsername());
            throw new BizException(ResultCode.LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("登录失败：密码错误 | userId={} | username={}", user.getId(), user.getUsername());
            throw new BizException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != UserStatus.ENABLED.getCode()) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }

        LoginUser loginUser = userPermissionService.loadLoginUser(user.getId());
        recordLogin(user.getId(), clientIp);

        LoginVO loginVO = issueTokens(loginUser, user);
        log.info("登录成功 | userId={} | username={} | ip={}", user.getId(), user.getUsername(), clientIp);
        return loginVO;
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parse(refreshToken);
        } catch (ExpiredJwtException ex) {
            throw new BizException(ResultCode.REFRESH_TOKEN_INVALID, "登录状态已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BizException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        if (!jwtTokenProvider.isRefreshToken(claims)) {
            throw new BizException(ResultCode.REFRESH_TOKEN_INVALID, "请使用刷新令牌换取访问令牌");
        }

        Long userId = jwtTokenProvider.getUserId(claims);
        String storedJti = stringRedisTemplate.opsForValue().get(CacheKeys.authRefresh(userId));
        // 比对 Redis 中保存的 jti：登出、改密或已在别处刷新过的旧令牌都会失效
        if (!StringUtils.hasText(storedJti) || !storedJti.equals(claims.getId())) {
            throw new BizException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        LoginUser loginUser = userPermissionService.loadLoginUser(userId);
        if (loginUser == null) {
            throw new BizException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        SysUser user = userMapper.selectById(userId);
        return issueTokens(loginUser, user);
    }

    @Override
    public void logout(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return;
        }
        try {
            Claims claims = jwtTokenProvider.parse(accessToken);
            long remainingSeconds = jwtTokenProvider.getRemainingSeconds(claims);
            if (remainingSeconds > 0 && claims.getId() != null) {
                // 黑名单 TTL 与令牌剩余有效期一致，过期后自动清理，不会无限增长
                stringRedisTemplate.opsForValue().set(CacheKeys.authBlacklist(claims.getId()),
                        "1", Duration.ofSeconds(remainingSeconds));
            }
            Long userId = jwtTokenProvider.getUserId(claims);
            stringRedisTemplate.delete(CacheKeys.authRefresh(userId));
            log.info("用户登出 | userId={}", userId);
        } catch (Exception ex) {
            // 令牌本身已失效时，登出仍然算成功，避免前端陷入「登出失败」的死循环
            log.warn("登出时令牌解析失败，已忽略 | error={}", ex.getMessage());
        }
    }

    @Override
    public UserInfoVO currentUser() {
        LoginUser loginUser = requireLoginUser();
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        UserInfoVO userInfo = userConverter.toUserInfoVO(user);
        userInfo.setRoleCodes(loginUser.getRoleCodes());
        userInfo.setPermissions(loginUser.getPermissions());
        return userInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        LoginUser loginUser = requireLoginUser();
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_NOT_MATCH);
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PARAM_INVALID, "新密码不能与原密码相同");
        }

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);

        // 密码变更后，其他设备上已签发的刷新令牌立即失效
        stringRedisTemplate.delete(CacheKeys.authRefresh(user.getId()));
        userPermissionService.evict(user.getId());
        log.info("用户修改密码成功 | userId={}", user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UpdateProfileDTO dto) {
        LoginUser loginUser = requireLoginUser();
        assertEmailAvailable(dto.getEmail(), loginUser.getUserId());
        assertPhoneAvailable(dto.getPhone(), loginUser.getUserId());

        SysUser update = new SysUser();
        update.setId(loginUser.getUserId());
        update.setNickname(dto.getNickname());
        update.setEmail(normalize(dto.getEmail()));
        update.setPhone(normalize(dto.getPhone()));
        update.setAvatar(dto.getAvatar());
        userMapper.updateById(update);

        userPermissionService.evict(loginUser.getUserId());
    }

    @Override
    public LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    // ==================== 内部方法 ====================

    /**
     * 签发双令牌，并把刷新令牌的 jti 写入 Redis。
     * 刷新令牌采用「一次一换」：每次刷新都会覆盖 Redis 中的 jti，旧令牌随即作废。
     */
    private LoginVO issueTokens(LoginUser loginUser, SysUser user) {
        String accessToken = jwtTokenProvider.createAccessToken(loginUser);
        String refreshToken = jwtTokenProvider.createRefreshToken(loginUser.getUserId());
        Claims refreshClaims = jwtTokenProvider.parse(refreshToken);
        stringRedisTemplate.opsForValue().set(CacheKeys.authRefresh(loginUser.getUserId()),
                refreshClaims.getId(), Duration.ofSeconds(jwtProperties.getRefreshTokenExpireSeconds()));

        UserInfoVO userInfo = userConverter.toUserInfoVO(user);
        userInfo.setRoleCodes(loginUser.getRoleCodes());
        userInfo.setPermissions(loginUser.getPermissions());

        return LoginVO.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessTokenExpireSeconds())
                .userInfo(userInfo)
                .build();
    }

    private void recordLogin(Long userId, String clientIp) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setLastLoginAt(LocalDateTime.now());
        update.setLastLoginIp(clientIp == null ? "" : clientIp);
        userMapper.updateById(update);
    }

    private void assertUsernameAvailable(String username) {
        Long count = userMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
    }

    private void assertEmailAvailable(String email, Long excludeUserId) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        Long count = userMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getEmail, email)
                .ne(excludeUserId != null, SysUser::getId, excludeUserId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.EMAIL_EXISTS);
        }
    }

    private void assertPhoneAvailable(String phone, Long excludeUserId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        Long count = userMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getPhone, phone)
                .ne(excludeUserId != null, SysUser::getId, excludeUserId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.PHONE_EXISTS);
        }
    }

    /** 空串统一转为 null，避免 (email, deleted) 唯一索引把多个"未填邮箱"当成重复 */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}