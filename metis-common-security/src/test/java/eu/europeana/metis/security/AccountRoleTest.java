package eu.europeana.metis.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AccountRoleTest {

  @Test
  void toStringRepresentation() {
    assertEquals("admin", AccountRole.ADMIN.toString());
    assertEquals("data-officer", AccountRole.DATA_OFFICER.toString());
  }
}