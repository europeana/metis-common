package eu.europeana.metis.security.test;

import static eu.europeana.metis.security.AccountRole.ADMIN;
import static eu.europeana.metis.security.AccountRole.DATA_OFFICER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtUtilsTest {

  private static final List<String> RESOURCES = List.of("test-resource");
  private JwtUtils jwtUtils;

  @BeforeEach
  void setUp() {
    jwtUtils = new JwtUtils(RESOURCES);
  }

  @Test
  void shouldCreateAdminJwtWithCorrectRoles() {
    Jwt jwt = jwtUtils.getAdminJwt();
    assertEquals(JwtUtils.MOCK_VALID_TOKEN, jwt.getTokenValue());
    assertHasRole(jwt, "test-resource", ADMIN.toString());
  }

  @Test
  void shouldCreateDataOfficerJwtWithCorrectRoles() {
    Jwt jwt = jwtUtils.getDataOfficerJwt();
    assertEquals(JwtUtils.MOCK_VALID_TOKEN, jwt.getTokenValue());
    assertHasRole(jwt, "test-resource", DATA_OFFICER.toString());
  }

  @Test
  void shouldCreateInvalidRoleJwt() {
    Jwt jwt = jwtUtils.getInvalidRoleJwt();
    assertEquals(JwtUtils.MOCK_INVALID_TOKEN, jwt.getTokenValue());
    assertHasRole(jwt, "test-resource", "INVALID");
  }

  @Test
  void shouldCreateEmptyRoleJwt() {
    Jwt jwt = jwtUtils.getEmptyRoleJwt();
    assertEquals(JwtUtils.MOCK_INVALID_TOKEN, jwt.getTokenValue());
    assertTrue(getRoles(jwt, "test-resource").isEmpty());
  }

  @Test
  void shouldCreateJwtWithNoUserId() {
    Jwt jwt = jwtUtils.getJwtNoUserId();
    assertNull(jwt.getClaim("sub"));
  }

  @Test
  void shouldCreateJwtWithEmptyStringUserId() {
    Jwt jwt = jwtUtils.getJwtWithEmptyStringUserId();
    assertNull(jwt.getClaim("sub"));
  }

  @Test
  void shouldContainExpectedStandardClaims() {
    Jwt jwt = jwtUtils.getAdminJwt();
    assertEquals("user@example.com", jwt.getClaim("email"));
    assertEquals("userName", jwt.getClaim("preferred_username"));
    assertEquals("firstName", jwt.getClaim("given_name"));
    assertEquals("lastName", jwt.getClaim("family_name"));
    assertInstanceOf(Instant.class, jwt.getClaim("iat"));
  }

  private void assertHasRole(Jwt jwt, String resource, String expectedRole) {
    List<String> roles = getRoles(jwt, resource);
    assertEquals(1, roles.size());
    assertEquals(expectedRole, roles.getFirst());
  }

  private List<String> getRoles(Jwt jwt, String resource) {
    Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
    if (resourceAccess == null || !resourceAccess.containsKey(resource)) {
      return List.of();
    }
    Map<String, Object> resourceMap = (Map<String, Object>) resourceAccess.get(resource);
    return (List<String>) resourceMap.getOrDefault("roles", List.of());
  }
}
