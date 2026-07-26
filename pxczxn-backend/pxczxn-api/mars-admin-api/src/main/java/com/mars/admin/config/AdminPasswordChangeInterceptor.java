package com.mars.admin.config;

import cn.dev33.satoken.stp.StpUtil;
import com.mars.common.exception.BusinessException;
import com.mars.system.entity.SysUser;
import com.mars.system.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Prevents an administrator with a one-time password from using protected APIs.
 */
@Component
@RequiredArgsConstructor
public class AdminPasswordChangeInterceptor implements HandlerInterceptor {

    private static final Set<String> ALLOWED_WHILE_PASSWORD_CHANGE_IS_REQUIRED = Set.of(
            "/api/auth/info",
            "/api/auth/logout",
            "/api/auth/password",
            "/api/crypto/config"
    );

    private final SysUserService userService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())
                || ALLOWED_WHILE_PASSWORD_CHANGE_IS_REQUIRED.contains(request.getRequestURI())
                || !StpUtil.isLogin()) {
            return true;
        }

        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        if (user != null && Integer.valueOf(1).equals(user.getMustChangePassword())) {
            throw new BusinessException(
                    428,
                    "首次登录或密码重置后必须先修改密码"
            );
        }
        return true;
    }
}
