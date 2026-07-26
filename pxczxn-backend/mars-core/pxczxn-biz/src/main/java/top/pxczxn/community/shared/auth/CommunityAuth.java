package top.pxczxn.community.shared.auth;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.stereotype.Component;

/**
 * 社区用户独立会话入口。
 *
 * <p>管理员仍使用脚手架原有的 {@code StpUtil}，社区业务只能通过本入口
 * 读写登录态，避免两套账号误用同一个登录类型或 Token 名称。</p>
 */
@Component
public final class CommunityAuth {

    public static final String LOGIN_TYPE = "community";
    public static final String TOKEN_NAME = "pxczxn-community-token";

    private final StpLogic stpLogic;

    public CommunityAuth() {
        SaTokenConfig config = new SaTokenConfig();
        config.setTokenName(TOKEN_NAME);
        config.setTimeout(7 * 24 * 60 * 60);
        config.setActiveTimeout(30 * 60);
        config.setIsConcurrent(true);
        config.setIsShare(false);
        config.setTokenStyle("uuid");
        config.setIsReadHeader(true);
        config.setIsReadBody(false);
        config.setIsReadCookie(false);

        stpLogic = new StpLogic(LOGIN_TYPE);
        stpLogic.setConfig(config);
        SaManager.putStpLogic(stpLogic);
    }

    public StpLogic stpLogic() {
        return stpLogic;
    }

    public void login(Long userId) {
        stpLogic.login(userId);
    }

    public void checkLogin() {
        stpLogic.checkLogin();
    }

    public boolean isLogin() {
        return stpLogic.isLogin();
    }

    public Long getOptionalLoginUserId() {
        return isLogin() ? stpLogic.getLoginIdAsLong() : null;
    }

    public Long getLoginUserId() {
        checkLogin();
        return stpLogic.getLoginIdAsLong();
    }

    public String getTokenValue() {
        return stpLogic.getTokenValue();
    }

    public long getTokenTimeout() {
        return stpLogic.getTokenTimeout();
    }

    public void logout() {
        stpLogic.logout();
    }
}
