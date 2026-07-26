package top.pxczxn.platform.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 前缀配置
 * 给旧后台控制器添加 /api 前缀，避免与前端路由冲突。
 * 社区控制器自行声明 /api/v1 前缀，不参与这里的旧脚手架规则。
 */
@Configuration
public class ApiPrefixConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", c ->
                c.isAnnotationPresent(RestController.class)
                        && c.getPackageName().startsWith("top.pxczxn.platform"));
    }
}
