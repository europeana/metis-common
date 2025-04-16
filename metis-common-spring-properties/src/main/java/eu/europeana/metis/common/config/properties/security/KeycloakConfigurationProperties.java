package eu.europeana.metis.common.config.properties.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Class using {@link ConfigurationProperties} loading.
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakConfigurationProperties(String authServerUrl, String clientId, String clientSecret, String realm) {}
