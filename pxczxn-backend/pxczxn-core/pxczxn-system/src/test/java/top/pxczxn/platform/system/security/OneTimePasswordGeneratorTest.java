package top.pxczxn.platform.system.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OneTimePasswordGeneratorTest {

    @Test
    void generatesUniquePasswordsWithEveryRequiredCharacterClass() {
        OneTimePasswordGenerator generator = new OneTimePasswordGenerator();
        Set<String> generated = new HashSet<>();

        for (int index = 0; index < 100; index++) {
            String password = generator.generate();
            assertThat(password).hasSize(16);
            assertThat(password).matches(".*[A-Z].*");
            assertThat(password).matches(".*[a-z].*");
            assertThat(password).matches(".*[0-9].*");
            assertThat(password).matches(".*[!@#$%*_\\-].*");
            generated.add(password);
        }

        assertThat(generated).hasSize(100);
    }
}
