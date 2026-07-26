package top.pxczxn.community.web.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Community HTTP authentication boundary.
 */
@Configuration
@RequiredArgsConstructor
public class CommunitySecurityConfig implements WebMvcConfigurer {

    private final CommunityAuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/api/v1/**")
                .order(10);
    }
}
