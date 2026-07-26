package com.mars.system.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token authentication and cross-origin configuration.
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            SaRouter.match("/api/**")
                    .notMatch(
                            "/api/auth/login",
                            "/api/auth/register",
                            "/api/auth/captcha",
                            "/api/auth/sms-code",
                            "/api/v1/**",
                            "/api/app/auth/login",
                            "/api/app/auth/sms-code",
                            "/api/wechat/miniprogram/**",
                            "/api/mall/home",
                            "/api/mall/login",
                            "/api/mall/loginByPhone",
                            "/api/crypto/**",
                            "/api/sys/config-group/public",
                            "/api/sys/user/template",
                            "/api/sys/file/preview/**",
                            "/api/sys/file/download/**",
                            "/api/file/**",
                            "/api/files/**"
                    )
                    .check(r -> StpUtil.checkLogin());
            SaRouter.match("/admin-api/**")
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/api/**", "/admin-api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
