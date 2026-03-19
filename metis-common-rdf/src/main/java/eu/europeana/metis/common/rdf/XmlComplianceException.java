package eu.europeana.metis.common.rdf;

import java.io.Serial;

/**
 * Thrown when a noncompliance with XML detected. It is not guaranteed to be thrown for any
 * noncompliant input, but it will be thrown if some noncompliance prevents making some change or
 * performing some desired analysis.
 */
public class XmlComplianceException extends ComplianceException{

  @Serial
  private static final long serialVersionUID = 7256922643844805751L;

  public XmlComplianceException(String message, Throwable cause) {
    super(message, cause);
  }
}
