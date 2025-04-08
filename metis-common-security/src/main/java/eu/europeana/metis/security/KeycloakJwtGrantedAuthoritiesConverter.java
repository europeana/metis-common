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

public class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  public static final String REALM_ACCESS = "realm_access";
  public static final String RESOURCE_ACCESS = "resource_access";
  public static final String ROLES = "roles";
  public static final String ROLE_PREFIX = "ROLE_";
  private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
  private final List<String> resourceNames;

  public KeycloakJwtGrantedAuthoritiesConverter(List<String> resourceNames) {
    this.resourceNames = resourceNames;
  }

  @Override
  public AbstractAuthenticationToken convert(@NotNull Jwt jwt) {
    Collection<GrantedAuthority> grantedAuthorities = Optional.of(defaultGrantedAuthoritiesConverter.convert(jwt))
                                                              .orElseGet(List::of);

    final Map<String, List<String>> realmAccess = jwt.getClaim(REALM_ACCESS);
    final List<String> realmRoles = (realmAccess == null) ? List.of() : realmAccess.getOrDefault(ROLES, List.of());

    final Map<String, Map<String, List<String>>> resourceAccess = jwt.getClaim(RESOURCE_ACCESS);

    for (String resourceName : resourceNames) {
      final List<String> resourceRoles = (resourceAccess == null) ? List.of()
          : resourceAccess.getOrDefault(resourceName, Map.of()).getOrDefault(ROLES, List.of());
      grantedAuthorities.addAll(getAuthorities(resourceRoles));

    }
    grantedAuthorities.addAll(getAuthorities(realmRoles));
    return new JwtAuthenticationToken(jwt, grantedAuthorities);
  }

  private static List<SimpleGrantedAuthority> getAuthorities(List<String> resourceRoles) {
    return resourceRoles.stream()
                        .map(role -> ROLE_PREFIX + role)
                        .map(SimpleGrantedAuthority::new)
                        .toList();
  }
}
