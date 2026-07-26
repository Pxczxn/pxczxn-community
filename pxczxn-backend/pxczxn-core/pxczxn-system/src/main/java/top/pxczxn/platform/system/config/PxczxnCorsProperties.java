package top.pxczxn.platform.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Explicit browser origins trusted by the community and administration clients.
 */
@Component
@ConfigurationProperties(prefix = "pxczxn.security.cors")
public class PxczxnCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null
                ? new ArrayList<>()
                : new ArrayList<>(allowedOrigins);
    }

    public List<String> validatedAllowedOrigins() {
        LinkedHashSet<String> validated = new LinkedHashSet<>();
        for (String candidate : allowedOrigins) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            String origin = candidate.trim();
            if (origin.contains("*")) {
                throw new IllegalStateException(
                        "pxczxn.security.cors.allowed-origins must not contain wildcards");
            }
            URI uri;
            try {
                uri = URI.create(origin);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Invalid CORS origin: " + origin, exception);
            }
            if (!("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    || !StringUtils.hasText(uri.getHost())
                    || uri.getPath() != null && !uri.getPath().isEmpty()) {
                throw new IllegalStateException("CORS origins must be absolute HTTP origins: " + origin);
            }
            validated.add(origin);
        }
        if (validated.isEmpty()) {
            throw new IllegalStateException(
                    "pxczxn.security.cors.allowed-origins must contain at least one explicit origin");
        }
        return List.copyOf(validated);
    }

    public String[] validatedAllowedOriginsArray() {
        return validatedAllowedOrigins().toArray(String[]::new);
    }
}
