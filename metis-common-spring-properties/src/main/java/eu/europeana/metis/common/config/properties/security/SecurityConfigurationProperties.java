package eu.europeana.metis.common.config.properties.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Class using {@link ConfigurationProperties} loading for custom security properties.
 */
@ConfigurationProperties(prefix = "security.oauth2")
public record SecurityConfigurationProperties(List<String> resourceNames) {}
