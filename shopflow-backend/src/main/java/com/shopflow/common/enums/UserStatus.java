package com.shopflow.common.enums;

import lombok.Getter;

/**
 * 用户账号状态。
 *
 * @author shopflow
 */
@Getter
public enum UserStatus {

    /** 禁用：不可登录，已登录的令牌也会被拒绝 */
    DISABLED(0, "禁用"),

    /** 启用：正常使用 */
    ENABLED(1, "启用");

    private final int code;

    private final String description;

    UserStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public boolean isEnabled() {
        return this == ENABLED;
    }
}