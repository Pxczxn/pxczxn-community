package top.pxczxn.community.web.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommunityHealthControllerTest {

    @Test
    void returnsOnlyPublicModuleStatus() {
        CommunityHealthController controller = new CommunityHealthController("test-version");

        var result = controller.health();

        assertEquals(200, result.getCode());
        assertEquals("UP", result.getData().status());
        assertEquals("pxczxn-community", result.getData().module());
        assertEquals("test-version", result.getData().version());
    }
}
