package top.pxczxn.community.article.permission;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

@Component
public class SaTokenPlatformArticleAuthority implements PlatformArticleAuthority {

    @Override
    public boolean isAuthenticated() {
        return StpUtil.isLogin();
    }

    @Override
    public boolean hasPermission(String permission) {
        return isAuthenticated() && StpUtil.hasPermission(permission);
    }
}
