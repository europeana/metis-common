package eu.europeana.metis.common.rdf;

import java.io.Serial;

/**
 * Thrown when a noncompliance with some standard is detected. It is not guaranteed to be thrown for
 * any noncompliant input, but it will be thrown if some noncompliance prevents making some change
 * or performing some desired analysis.
 */
public class ComplianceException extends Exception {

  @Serial
  private static final long serialVersionUID = -7888786324357364758L;

  public ComplianceException(String message) {
    super(message);
  }

  public ComplianceException(String message, Throwable cause) {
    super(message, cause);
  }
}
