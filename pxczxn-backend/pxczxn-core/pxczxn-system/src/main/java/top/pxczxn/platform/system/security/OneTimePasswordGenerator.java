package top.pxczxn.platform.system.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates administrator one-time passwords that satisfy every password class.
 */
@Component
public class OneTimePasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%*-_";
    private static final String ALL = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
    private static final int LENGTH = 16;

    private final SecureRandom secureRandom;

    public OneTimePasswordGenerator() {
        this(new SecureRandom());
    }

    OneTimePasswordGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate() {
        List<Character> characters = new ArrayList<>(LENGTH);
        characters.add(randomCharacter(UPPERCASE));
        characters.add(randomCharacter(LOWERCASE));
        characters.add(randomCharacter(DIGITS));
        characters.add(randomCharacter(SPECIAL));
        while (characters.size() < LENGTH) {
            characters.add(randomCharacter(ALL));
        }
        Collections.shuffle(characters, secureRandom);

        StringBuilder password = new StringBuilder(LENGTH);
        characters.forEach(password::append);
        return password.toString();
    }

    private char randomCharacter(String candidates) {
        return candidates.charAt(secureRandom.nextInt(candidates.length()));
    }
}
