package com.shopflow.controller;

import com.shopflow.common.constant.CommonConstants;
import com.shopflow.common.result.R;
import com.shopflow.common.util.IpUtils;
import com.shopflow.dto.auth.ChangePasswordDTO;
import com.shopflow.dto.auth.LoginDTO;
import com.shopflow.dto.auth.RefreshTokenDTO;
import com.shopflow.dto.auth.RegisterDTO;
import com.shopflow.dto.auth.UpdateProfileDTO;
import com.shopflow.security.LoginRequired;
import com.shopflow.service.AuthService;
import com.shopflow.vo.auth.LoginVO;
import com.shopflow.vo.auth.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * @author shopflow
 */
@Tag(name = "认证", description = "注册、登录、令牌刷新、个人中心")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@RequestBody @Valid RegisterDTO dto) {
        authService.register(dto);
        return R.ok();
    }

    @Operation(summary = "用户登录", description = "返回双令牌与用户信息")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        return R.ok(authService.login(dto, IpUtils.getClientIp(request)));
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestBody @Valid RefreshTokenDTO dto) {
        return R.ok(authService.refresh(dto.getRefreshToken()));
    }

    @Operation(summary = "登出")
    @LoginRequired
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        authService.logout(resolveToken(request));
        return R.ok();
    }

    @Operation(summary = "当前登录用户信息与权限")
    @LoginRequired
    @GetMapping("/me")
    public R<UserInfoVO> currentUser() {
        return R.ok(authService.currentUser());
    }

    @Operation(summary = "修改密码", description = "修改成功后其他设备的登录状态会失效")
    @LoginRequired
    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return R.ok();
    }

    @Operation(summary = "修改个人资料")
    @LoginRequired
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody @Valid UpdateProfileDTO dto) {
        authService.updateProfile(dto);
        return R.ok();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(CommonConstants.AUTH_HEADER);
        if (header == null || !header.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return null;
        }
        return header.substring(CommonConstants.TOKEN_PREFIX.length()).trim();
    }
}