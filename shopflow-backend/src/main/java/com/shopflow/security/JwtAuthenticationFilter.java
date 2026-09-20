package com.shopflow.security;

import com.shopflow.common.constant.CacheKeys;
import com.shopflow.common.constant.CommonConstants;
import com.shopflow.common.enums.UserStatus;
import com.shopflow.service.UserPermissionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器。
 *
 * <p>执行流程：
 * <ol>
 *     <li>从 Authorization 头取出 Bearer 令牌，没有令牌直接放行（由后续授权决策决定是否需要登录）；</li>
 *     <li>校验签名与有效期，失败则记录原因后放行，最终由 RestAuthenticationEntryPoint 返回统一的 401；</li>
 *     <li>校验令牌类型必须是 access，避免用长效刷新令牌直接访问业务接口；</li>
 *     <li>校验 jti 是否在登出黑名单中；</li>
 *     <li>加载登录用户（权限来自 Redis 缓存），账号被禁用则不建立认证上下文。</li>
 * </ol>
 *
 * <p>注意：过滤器只负责「认证」，不负责「授权」。
 * 是否需要权限由 SecurityConfig 的规则与 @RequirePermission 注解决定。
 *
 * @author shopflow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 请求属性：认证失败原因，供统一 401 响应区分「过期」与「非法」 */
    public static final String ATTR_JWT_ERROR = "shopflow.jwt.error";

    /** 认证失败原因：令牌过期 */
    public static final String ERROR_EXPIRED = "expired";

    /** 认证失败原因：令牌非法 */
    public static final String ERROR_INVALID = "invalid";

    /** 认证失败原因：令牌已登出 */
    public static final String ERROR_REVOKED = "revoked";

    private final JwtTokenProvider jwtTokenProvider;

    private final UserPermissionService userPermissionService;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            authenticate(request, token);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parse(token);
        } catch (ExpiredJwtException ex) {
            request.setAttribute(ATTR_JWT_ERROR, ERROR_EXPIRED);
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(ATTR_JWT_ERROR, ERROR_INVALID);
            return;
        }

        if (!jwtTokenProvider.isAccessToken(claims)) {
            request.setAttribute(ATTR_JWT_ERROR, ERROR_INVALID);
            return;
        }

        String jti = claims.getId();
        if (jti != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(CacheKeys.authBlacklist(jti)))) {
            request.setAttribute(ATTR_JWT_ERROR, ERROR_REVOKED);
            return;
        }

        Long userId = jwtTokenProvider.getUserId(claims);
        LoginUser loginUser = userPermissionService.loadLoginUser(userId);
        if (loginUser == null) {
            request.setAttribute(ATTR_JWT_ERROR, ERROR_INVALID);
            return;
        }
        if (loginUser.getStatus() == null || loginUser.getStatus() != UserStatus.ENABLED.getCode()) {
            log.warn("已禁用账号尝试访问 | userId={} | uri={}", userId, request.getRequestURI());
            request.setAttribute(ATTR_JWT_ERROR, ERROR_INVALID);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(CommonConstants.AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return null;
        }
        String token = header.substring(CommonConstants.TOKEN_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }
}