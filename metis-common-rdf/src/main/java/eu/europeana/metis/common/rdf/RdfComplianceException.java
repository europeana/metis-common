package eu.europeana.metis.common.rdf;

import java.io.Serial;

/**
 * Thrown when a noncompliance with RDF is detected. It is not guaranteed to be thrown for any
 * noncompliant input, but it will be thrown if some noncompliance prevents making some change or
 * performing some desired analysis.
 */
public class RdfComplianceException extends ComplianceException {

  @Serial
  private static final long serialVersionUID = -6579992905661241969L;

  public RdfComplianceException(String message) {
    super(message);
  }
}
