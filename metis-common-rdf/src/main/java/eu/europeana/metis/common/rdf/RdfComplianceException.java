package eu.europeana.metis.common.rdf;

import java.io.Serial;

public class RdfComplianceException extends Exception {

  @Serial
  private static final long serialVersionUID = -6579992905661241969L;

  public RdfComplianceException(String message) {
    super(message);
  }
}
