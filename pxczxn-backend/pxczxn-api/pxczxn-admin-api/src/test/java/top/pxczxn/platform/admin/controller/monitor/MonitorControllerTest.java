package top.pxczxn.platform.admin.controller.monitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MonitorControllerTest {

    @Test
    void jdbcUrlSanitizationRedactsEveryCredentialParameter() {
        String result = MonitorController.sanitizeJdbcUrl(
                "jdbc:mysql://localhost:3306/app?user=root&password=secret&useSSL=false"
        );

        assertThat(result)
                .contains("user=***", "password=***", "useSSL=false")
                .doesNotContain("root", "secret");
    }
}
