package com.shopflow.security;

import com.shopflow.common.constant.CommonConstants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 登录用户上下文工具类。
 *
 * <p>业务代码通过它获取当前登录人，避免在方法签名里层层传递 userId。
 * 未登录时统一降级为「系统操作人 0」，保证定时任务等无上下文场景也能正常执行。
 *
 * @author shopflow
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 获取当前登录用户，未登录返回 null */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return (principal instanceof LoginUser loginUser) ? loginUser : null;
    }

    /** 获取当前登录用户 ID，未登录返回系统操作人 ID */
    public static Long getUserIdOrZero() {
        LoginUser loginUser = getLoginUser();
        return (loginUser == null || loginUser.getUserId() == null)
                ? CommonConstants.SYSTEM_USER_ID
                : loginUser.getUserId();
    }

    /** 获取当前登录用户名，未登录返回空串 */
    public static String getUsernameOrEmpty() {
        LoginUser loginUser = getLoginUser();
        return (loginUser == null || loginUser.getUsername() == null) ? "" : loginUser.getUsername();
    }

    /** 当前是否已登录 */
    public static boolean isLogin() {
        return getLoginUser() != null;
    }
}