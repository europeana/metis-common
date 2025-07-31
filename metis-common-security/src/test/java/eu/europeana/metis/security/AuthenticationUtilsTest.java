package eu.europeana.metis.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AuthenticationUtilsTest {

  private Jwt jwt;
  private final String userId = "userId";
  private final String userName = "userName";
  private final String firstName = "firstName";
  private final String lastName = "lastName";
  private final Instant issuedAt = Instant.now();

  @BeforeEach
  void setup() {
    jwt = Jwt.withTokenValue("mock-token")
             .header("alg", "none")
             .claim("sub", userId)
             .claim("preferred_username", userName)
             .claim("given_name", firstName)
             .claim("family_name", lastName)
             .issuedAt(issuedAt)
             .build();
  }

  @Test
  void shouldReturnUserIdFromJwt() {
    String extractedUserId = AuthenticationUtils.getUserId(jwt);
    assertEquals(userId, extractedUserId);
  }

  @Test
  void shouldReturnUserNameFromJwt() {
    String extractedUserName = AuthenticationUtils.getUserName(jwt);
    assertEquals(userName, extractedUserName);
  }

  @Test
  void shouldReturnFirstNameFromJwt() {
    String extractedFirstName = AuthenticationUtils.getFirstName(jwt);
    assertEquals(firstName, extractedFirstName);
  }

  @Test
  void shouldReturnLastNameFromJwt() {
    String extractedLastName = AuthenticationUtils.getLastName(jwt);
    assertEquals(lastName, extractedLastName);
  }

  @Test
  void shouldReturnIssuedAtFromJwt() {
    Instant extractedIssuedAt = AuthenticationUtils.getIssuedAt(jwt);
    assertEquals(issuedAt, extractedIssuedAt);
  }

  @Test
  void shouldReturnNullIfClaimsMissing() {
    Jwt emptyJwt = Jwt.withTokenValue("empty-token")
                      .header("alg", "none")
                      .claim("someName", "someValue")
                      .build();

    assertNull(AuthenticationUtils.getUserId(emptyJwt));
    assertNull(AuthenticationUtils.getUserName(emptyJwt));
    assertNull(AuthenticationUtils.getFirstName(emptyJwt));
    assertNull(AuthenticationUtils.getLastName(emptyJwt));
    assertNull(AuthenticationUtils.getIssuedAt(emptyJwt));
  }
}