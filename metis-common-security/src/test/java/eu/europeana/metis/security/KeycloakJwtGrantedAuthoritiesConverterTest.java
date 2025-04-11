package eu.europeana.metis.security;

import static eu.europeana.metis.security.KeycloakJwtGrantedAuthoritiesConverter.REALM_ACCESS;
import static eu.europeana.metis.security.KeycloakJwtGrantedAuthoritiesConverter.RESOURCE_ACCESS;
import static eu.europeana.metis.security.KeycloakJwtGrantedAuthoritiesConverter.ROLES;
import static eu.europeana.metis.security.KeycloakJwtGrantedAuthoritiesConverter.ROLE_PREFIX;
import static eu.europeana.metis.security.KeycloakJwtGrantedAuthoritiesConverter.prefixRole;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class KeycloakJwtGrantedAuthoritiesConverterTest {

  public static final String RESOURCE_NAME1 = "resource1";
  private KeycloakJwtGrantedAuthoritiesConverter keycloakJwtGrantedAuthoritiesConverter;
  private Jwt jwt;

  @BeforeEach
  void setUp() {
    keycloakJwtGrantedAuthoritiesConverter = new KeycloakJwtGrantedAuthoritiesConverter(List.of(RESOURCE_NAME1), true);
    jwt = mock(Jwt.class);
  }

  @Test
  void convertRealmRolesToAuthorities() {
    when(jwt.getClaim(REALM_ACCESS)).thenReturn(Map.of(ROLES, List.of("admin", "user")));
    when(jwt.getClaim(RESOURCE_ACCESS)).thenReturn(null);

    JwtAuthenticationToken token = (JwtAuthenticationToken) keycloakJwtGrantedAuthoritiesConverter.convert(jwt);

    assertNotNull(token);
    assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(REALM_ACCESS, "admin")))));
    assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(REALM_ACCESS, "user")))));
    assertFalse(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(RESOURCE_NAME1, "admin")))));
    assertFalse(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(RESOURCE_NAME1, "user")))));
  }

  @Test
  void convertPrefixRolesToAuthorities() {
    when(jwt.getClaim(REALM_ACCESS)).thenReturn(null);
    when(jwt.getClaim(RESOURCE_ACCESS)).thenReturn(Map.of(RESOURCE_NAME1, Map.of(ROLES, List.of("admin", "user"))));

    JwtAuthenticationToken token = (JwtAuthenticationToken) keycloakJwtGrantedAuthoritiesConverter.convert(jwt);

    assertNotNull(token);
    assertFalse(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(REALM_ACCESS, "admin")))));
    assertFalse(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(REALM_ACCESS, "user")))));
    assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(RESOURCE_NAME1, "admin")))));
    assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(RESOURCE_NAME1, "user")))));
  }

  @Test
  void convertBothRealmAndPrefixRolesToAuthorities() {
    when(jwt.getClaim(REALM_ACCESS)).thenReturn(Map.of(ROLES, List.of("admin", "user")));
    when(jwt.getClaim(RESOURCE_ACCESS)).thenReturn(Map.of(RESOURCE_NAME1, Map.of(ROLES, List.of("admin", "user"))));

    JwtAuthenticationToken token = (JwtAuthenticationToken) keycloakJwtGrantedAuthoritiesConverter.convert(jwt);

    assertNotNull(token);
    assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(REALM_ACCESS, "admin")))));
    assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(REALM_ACCESS, "user")))));
    assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(RESOURCE_NAME1, "admin")))));
    assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(
        prefixRole(ROLE_PREFIX, prefixRole(RESOURCE_NAME1, "user")))));
  }

  @Test
  void convertHandleMissingRoles() {
    when(jwt.getClaim(REALM_ACCESS)).thenReturn(null);
    when(jwt.getClaim(RESOURCE_ACCESS)).thenReturn(null);

    JwtAuthenticationToken token = (JwtAuthenticationToken) keycloakJwtGrantedAuthoritiesConverter.convert(jwt);

    assertNotNull(token);
    assertTrue(token.getAuthorities().isEmpty());
  }

  @Test
  void convertHandleMissingResourceNames() {
    KeycloakJwtGrantedAuthoritiesConverter keycloakJwtGrantedAuthoritiesConverterEmptyList = new KeycloakJwtGrantedAuthoritiesConverter(List.of(), false);
    when(jwt.getClaim(REALM_ACCESS)).thenReturn(Map.of(ROLES, List.of("admin", "user")));
    when(jwt.getClaim(RESOURCE_ACCESS)).thenReturn(Map.of(RESOURCE_NAME1, Map.of(ROLES, List.of("admin", "user"))));

    JwtAuthenticationToken token = (JwtAuthenticationToken) keycloakJwtGrantedAuthoritiesConverterEmptyList.convert(jwt);

    assertNotNull(token);
    assertTrue(token.getAuthorities().isEmpty());
  }

  @Test
  void buildResourceRoles(){
    String[] resourceRoles = KeycloakJwtGrantedAuthoritiesConverter.buildResourceRoles(List.of(RESOURCE_NAME1),
        List.of("admin", "user"));
    assertArrayEquals(new String[]{prefixRole(RESOURCE_NAME1, "admin"), prefixRole(RESOURCE_NAME1, "user")}, resourceRoles);
  }
}