package com.shopflow.aspect;

import com.shopflow.common.result.ResultCode;
import com.shopflow.exception.BizException;
import com.shopflow.security.LoginUser;
import com.shopflow.security.RequirePermission;
import com.shopflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * 接口权限校验切面。
 *
 * <p>与 Spring Security 的分工：
 * <ul>
 *     <li>SecurityConfig 负责「是否登录」（认证）；</li>
 *     <li>本切面负责「有没有这个操作的权限」（授权）。</li>
 * </ul>
 *
 * <p>为什么不用 Spring Security 的 @PreAuthorize("hasAuthority('x')")？
 * 因为那要求权限字符串参与表达式解析，权限变更时调试成本高；
 * 这里用注解 + 显式集合判断，逻辑一目了然，也便于做 AND / OR 组合与日志埋点。
 *
 * @author shopflow
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        // 超级管理员视为拥有全部权限，避免新增权限码后忘记给 ADMIN 授权导致后台不可用
        if (loginUser.isAdmin()) {
            return;
        }

        String[] requiredCodes = requirePermission.value();
        Set<String> ownedCodes = loginUser.getPermissions();
        boolean pass = requirePermission.logical() == RequirePermission.Logical.AND
                ? Arrays.stream(requiredCodes).allMatch(ownedCodes::contains)
                : Arrays.stream(requiredCodes).anyMatch(ownedCodes::contains);

        if (!pass) {
            log.warn("权限校验未通过 | userId={} | username={} | required={} | owned={}",
                    loginUser.getUserId(), loginUser.getUsername(),
                    Arrays.toString(requiredCodes), ownedCodes);
            throw new BizException(ResultCode.FORBIDDEN,
                    "缺少操作权限: " + String.join(",", requiredCodes));
        }
    }
}