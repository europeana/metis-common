package eu.europeana.metis.security.test;

import eu.europeana.metis.security.AccountRole;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * This module is only intended for use in test code. It provides test data factories and shared test utilities.
 */

public class JwtUtils {

  public static final String BEARER = "Bearer ";
  public static final String MOCK_VALID_TOKEN = "xxx.yyy.zzz";
  public static final String MOCK_INVALID_TOKEN = "invalidToken";
  private final List<String> resourceNames;

  /**
   * Constructs a new instance of JwtUtils. This utility class is primarily used for generating and handling JWTs for testing
   * purposes, including creating tokens with various roles and user configurations.
   *
   * @param resourceNames A list of resource names that will be associated with the generated JWT tokens, representing the
   * resources the token has access to.
   */
  public JwtUtils(List<String> resourceNames) {
    this.resourceNames = List.copyOf(resourceNames);
  }

  public Jwt getAdminJwt() {
    return getJwt(MOCK_VALID_TOKEN, List.of(AccountRole.ADMIN.toString()));
  }

  public Jwt getDataOfficerJwt() {
    return getJwt(MOCK_VALID_TOKEN, List.of(AccountRole.DATA_OFFICER.toString()));
  }

  public Jwt getInvalidRoleJwt() {
    return getJwt(MOCK_INVALID_TOKEN, List.of("INVALID"));
  }

  public Jwt getEmptyRoleJwt() {
    return getJwt(MOCK_INVALID_TOKEN, List.of());
  }

  public Jwt getJwtNoUserId() {
    return getJwtNoUserId(MOCK_VALID_TOKEN, List.of(AccountRole.DATA_OFFICER.toString()));
  }

  public Jwt getJwtWithEmptyStringUserId() {
    return getJwtNoUserId(MOCK_VALID_TOKEN, List.of(AccountRole.DATA_OFFICER.toString()));
  }

  private @NotNull Jwt getJwt(String token, List<String> roles) {
    return getJwt(token, UUID.randomUUID().toString(), roles);
  }

  private @NotNull Jwt getJwtNoUserId(String token, List<String> roles) {
    return getJwt(token, null, roles);
  }

  private @NotNull Jwt getJwt(String token, String withUserId, List<String> roles) {
    Map<String, Object> resourceAccess = resourceNames.stream()
                                                      .collect(Collectors.toMap(name -> name, name -> Map.of("roles", roles)));
    return createJwtBuilder(token, withUserId, resourceAccess).build();
  }

  private static @NotNull Jwt.Builder createJwtBuilder(String token, String withUserId, Map<String, Object> resourceAccess) {
    return Jwt.withTokenValue(token)
              .header("alg", "none")
              .claim("sub", withUserId)
              .claim("resource_access", resourceAccess)
              .claim("email", "user@example.com")
              .claim("preferred_username", "userName")
              .claim("given_name", "firstName")
              .claim("family_name", "lastName")
              .claim("iat", Instant.now());
  }
}
