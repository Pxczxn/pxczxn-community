package top.pxczxn.platform.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.pxczxn.platform.system.config.ScaffoldFeatureProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaffoldFeatureInterceptorTest {

    private final ScaffoldFeatureProperties features = new ScaffoldFeatureProperties();
    private final ScaffoldFeatureInterceptor interceptor =
            new ScaffoldFeatureInterceptor(features, new ObjectMapper());

    @Test
    void legacyScaffoldEndpointsAreDisabledByDefault() throws Exception {
        List<String> disabledPaths = List.of(
                "/api/auth/sms-code",
                "/api/sys/config-group/test-payment",
                "/api/sys/config-group/test-sms",
                "/api/sys/config-group/sms-logs/recent",
                "/api/wechat/miniprogram/login",
                "/api/monitor/server-manager/list",
                "/api/tool/gen/page",
                "/api/system/customer/page",
                "/api/system/student/page",
                "/api/test/example"
        );

        for (String path : disabledPaths) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            MockHttpServletResponse response = new MockHttpServletResponse();

            assertFalse(interceptor.preHandle(request, response, new Object()), path);
            assertEquals(404, response.getStatus(), path);
        }
    }

    @Test
    void retainedEndpointsRemainAvailable() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(200, response.getStatus());
    }

    @Test
    void chatEndpointsAreAvailableWhenChatIsEnabled() throws Exception {
        features.setChat(true);

        for (String path : List.of(
                "/api/sys/chat/history/1",
                "/api/chat/group/list")) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("GET", path);
            MockHttpServletResponse response = new MockHttpServletResponse();

            assertTrue(interceptor.preHandle(request, response, new Object()));
            assertEquals(200, response.getStatus());
        }
    }
}
