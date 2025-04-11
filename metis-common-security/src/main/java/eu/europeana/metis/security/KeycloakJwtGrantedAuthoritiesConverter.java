package eu.europeana.metis.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Converts a {@link Jwt} token to an {@link AbstractAuthenticationToken} by extracting and converting the roles from the JWT
 * claims into {@link GrantedAuthority} instances. This converter specifically supports extracting roles from both the realm
 * access and resource access parts of a Keycloak JWT token.
 *
 * <p>The converter can be configured to include roles from the realm access part of the token, and
 * it also supports specifying which resources roles should be included from the resource access part.
 *
 * <p>Roles are prefixed with the resource name (for resource access roles) or a fixed prefix (for realm access roles)
 * to avoid conflicts and clearly differentiate the source of the authority.
 */
public class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  public static final String REALM_ACCESS = "realm_access";
  public static final String RESOURCE_ACCESS = "resource_access";
  public static final String ROLES = "roles";
  public static final String ROLE_PREFIX = "ROLE";
  private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
  private final List<String> grantedResources;
  private final boolean grantRealmAccessRoles;

  public KeycloakJwtGrantedAuthoritiesConverter(List<String> grantedResources, boolean grantRealmAccessRoles) {
    this.grantedResources = grantedResources;
    this.grantRealmAccessRoles = grantRealmAccessRoles;
  }

  @Override
  public AbstractAuthenticationToken convert(@NotNull Jwt jwt) {
    Collection<GrantedAuthority> grantedAuthorities = Optional.of(defaultGrantedAuthoritiesConverter.convert(jwt))
                                                              .orElseGet(List::of);

    if (grantRealmAccessRoles) {
      final Map<String, List<String>> realmAccess = jwt.getClaim(REALM_ACCESS);
      final List<String> realmRoles = (realmAccess == null) ? List.of() : realmAccess.getOrDefault(ROLES, List.of());
      grantedAuthorities.addAll(getAuthorities(REALM_ACCESS, realmRoles));
    }

    final Map<String, Map<String, List<String>>> resourceAccess = jwt.getClaim(RESOURCE_ACCESS);

    for (String grantedResource : grantedResources) {
      final List<String> resourceRoles = (resourceAccess == null) ? List.of()
          : resourceAccess.getOrDefault(grantedResource, Map.of()).getOrDefault(ROLES, List.of());
      grantedAuthorities.addAll(getAuthorities(grantedResource, resourceRoles));

    }
    return new JwtAuthenticationToken(jwt, grantedAuthorities);
  }

  private static List<SimpleGrantedAuthority> getAuthorities(String resourceName, List<String> roles) {
    return roles.stream()
                .map(role -> prefixRole(ROLE_PREFIX, prefixRole(resourceName, role)))
                .map(SimpleGrantedAuthority::new)
                .toList();
  }

  static @NotNull String prefixRole(String resourceName, String role) {
    return "%s_%s" .formatted(resourceName, role);
  }

  /**
   * Builds an array of role names prefixed with each of the given resource names.
   * <p>
   * This method is useful for generating a comprehensive list of role authorities from multiple resources.
   *
   * @param resourceNames a list of resource names to use as prefixes
   * @param roles a list of roles to be prefixed
   * @return an array of strings representing the prefixed role names
   */
  public static String[] buildResourceRoles(List<String> resourceNames, List<String> roles) {
    return resourceNames.stream()
                        .flatMap(resource -> roles.stream().map(role -> prefixRole(resource, role)))
                        .toArray(String[]::new);
  }
}
