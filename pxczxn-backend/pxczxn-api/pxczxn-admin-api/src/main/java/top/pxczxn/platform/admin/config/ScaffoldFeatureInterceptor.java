package top.pxczxn.platform.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.pxczxn.platform.common.result.Result;
import top.pxczxn.platform.system.config.ScaffoldFeatureProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 为保留但尚未启用的脚手架能力提供 HTTP 边界。
 */
@Component
@RequiredArgsConstructor
public class ScaffoldFeatureInterceptor implements HandlerInterceptor {

    private final ScaffoldFeatureProperties features;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {
        String uri = request.getRequestURI();
        if (isDisabled(uri)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    objectMapper.writeValueAsString(Result.fail(404, "功能未启用")));
            return false;
        }
        return true;
    }

    private boolean isDisabled(String uri) {
        if (!features.isSms() && (
                matches(uri, "/api/auth/sms-code")
                        || matches(uri, "/api/sys/config-group/test-sms")
                        || matches(uri, "/api/sys/config-group/sms-logs"))) {
            return true;
        }
        if (!features.isPayment()
                && matches(uri, "/api/sys/config-group/test-payment")) {
            return true;
        }
        if (!features.isWechat() && matches(uri, "/api/wechat")) {
            return true;
        }
        if (!features.isChat() && (
                matches(uri, "/api/sys/chat")
                        || matches(uri, "/api/chat/group"))) {
            return true;
        }
        if (!features.isSshServer()
                && matches(uri, "/api/monitor/server-manager")) {
            return true;
        }
        if (!features.isCodegen() && matches(uri, "/api/tool/gen")) {
            return true;
        }
        return !features.isSamples() && (
                matches(uri, "/api/system/customer")
                        || matches(uri, "/api/system/student")
                        || matches(uri, "/api/test"));
    }

    private boolean matches(String uri, String featurePath) {
        return uri.equals(featurePath) || uri.startsWith(featurePath + "/");
    }
}
