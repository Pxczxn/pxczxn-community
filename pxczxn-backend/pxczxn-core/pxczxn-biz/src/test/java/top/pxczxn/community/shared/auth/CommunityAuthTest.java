package top.pxczxn.community.shared.auth;

import cn.dev33.satoken.SaManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CommunityAuthTest {

    @Test
    void registersAnIndependentCommunityLoginTypeAndTokenName() {
        CommunityAuth communityAuth = new CommunityAuth();

        assertEquals(CommunityAuth.LOGIN_TYPE, communityAuth.stpLogic().getLoginType());
        assertEquals(CommunityAuth.TOKEN_NAME, communityAuth.stpLogic().getConfig().getTokenName());
        assertNotEquals("Authorization", communityAuth.stpLogic().getConfig().getTokenName());
        assertSame(communityAuth.stpLogic(), SaManager.getStpLogic(CommunityAuth.LOGIN_TYPE));
    }
}
