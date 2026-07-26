package top.pxczxn.platform.system.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PxczxnCorsPropertiesTest {

    @Test
    void acceptsDistinctExplicitHttpOrigins() {
        PxczxnCorsProperties properties = new PxczxnCorsProperties();
        properties.setAllowedOrigins(List.of(
                "http://localhost:8847",
                "https://community.example.com",
                "http://localhost:8847"
        ));

        assertThat(properties.validatedAllowedOrigins())
                .containsExactly(
                        "http://localhost:8847",
                        "https://community.example.com"
                );
    }

    @Test
    void rejectsWildcardAndEmptyOriginLists() {
        PxczxnCorsProperties wildcard = new PxczxnCorsProperties();
        wildcard.setAllowedOrigins(List.of("*"));
        assertThatThrownBy(wildcard::validatedAllowedOrigins)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wildcards");

        PxczxnCorsProperties empty = new PxczxnCorsProperties();
        assertThatThrownBy(empty::validatedAllowedOrigins)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least one");
    }
}
