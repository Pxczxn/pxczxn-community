package top.pxczxn.community.file.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunityFileInspectorTest {

    @Test
    void detectsImageFromBytesInsteadOfClaimedMime() {
        byte[] png = {
                (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'
        };

        CommunityFileInspector.InspectedFile inspected =
                CommunityFileInspector.inspect("avatar.png", png);

        assertThat(inspected.mimeType()).isEqualTo("image/png");
        assertThat(inspected.extension()).isEqualTo("png");
    }

    @Test
    void rejectsExtensionAndMagicMismatch() {
        byte[] png = {
                (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'
        };

        assertThatThrownBy(() -> CommunityFileInspector.inspect("avatar.jpg", png))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("真实类型");
    }

    @Test
    void rejectsScriptAndEventHandlersInSvg() {
        byte[] svg = """
                <svg xmlns="http://www.w3.org/2000/svg" onload="alert(1)">
                  <script>alert(1)</script>
                </svg>
                """.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> CommunityFileInspector.inspect("unsafe.svg", svg))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不安全");
    }
}
