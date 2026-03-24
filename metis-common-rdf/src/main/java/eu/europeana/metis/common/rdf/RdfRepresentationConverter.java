package eu.europeana.metis.common.rdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;

/**
 * <p>
 * This class converts content from one representation format to another. This functionality is not
 * streamed, and can thus not handle large amounts of data. Note that input is required to be
 * valid RDF+XML. Specifically, this means that this functionality will not accept data that has
 * the non-RDF-compliant provenance attributes <code>edm:wasGeneratedBy</code> and
 * <code>edm:confidenceLevel</code>.
 * </p>
 * <p>
 * A note on implementation: a streamed solution exists as part of Jena RIOT:
 * </p>
 * <p>
 * <code>RDFParser.create().source(...).parse(StreamRDFWriter.getWriterStream(...));</code>
 * </p>
 * <p>
 * The problem is that this method does not support XML writing, and specifically not the type of
 * normalization we implement (i.e., the flattening). For this reason, this class implements a more
 * generic but less efficient method that is designed to work with small amounts of data (like
 * individual records and contextual items).
 * </p>
 */
public final class RdfRepresentationConverter {

  private RdfRepresentationConverter() {
  }

  /**
   * Convert RDF content from one representation to another. Note that this conversion is performed
   * even if the two representations are identical. A call to this method can then serve to
   * verify the content and achieve more consistency (a kind of 'normalization').
   *
   * @param content The content as a String.
   * @param from    The representation of the source.
   * @param to      The required representation of the result.
   * @param baseUrl The base URL for the content. Can be <code>null</code>, in which case an
   *                exception is thrown if relative URLs are encountered.
   * @return The content in the new representation.
   * @throws ComplianceException When input was found to be noncompliant.
   */
  public static String convertRdf(String content, RdfRepresentation from, RdfRepresentation to,
      String baseUrl) throws ComplianceException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    convertRdf(new ByteArrayInputStream(content.getBytes()), from, to, baseUrl, outputStream);
    return outputStream.toString();
  }

  /**
   * Convert RDF content from one representation to another. Note that this conversion is performed
   * even if the two representations are identical. A call to this method can then serve to verify
   * the content and achieve more consistency (a kind of 'normalization').
   *
   * @param content The content as an input stream.
   * @param from    The representation of the source.
   * @param to      The required representation of the result.
   * @param baseUrl The base URL for the content. Can be <code>null</code>, in which case an
   *                exception is thrown if relative URLs are encountered.
   * @param result  The stream where the result is to be written.
   * @throws ComplianceException When input was found to be noncompliant.
   */
  public static void convertRdf(InputStream content, RdfRepresentation from, RdfRepresentation to,
      String baseUrl, OutputStream result) throws ComplianceException {

    // Read the data to a model.
    final String nonNullBaseUrl = Optional.ofNullable(baseUrl)
        .filter(StringUtils::isNotBlank).orElse(RdfBaseUrlUtils.DEFAULT_BASE_URL);
    final Model model = ModelFactory.createDefaultModel();
    try {
      RDFDataMgr.read(model, content, nonNullBaseUrl, from.getLang());
    } catch (RiotException e) {
      throw new ComplianceException("Input was found to be noncompliant. " + e.getMessage(), e);
    }

    // Check if there were relative URLs without a provided base URL.
    if (RdfBaseUrlUtils.DEFAULT_BASE_URL.equals(nonNullBaseUrl) &&
        RdfBaseUrlUtils.containsDefaultBaseUrl(model)) {
      throw new ComplianceException(
          "This data has relative URLs and a base URL should be provided to convert it.");
    }

    // Done. Write the result.
    RDFDataMgr.write(result, model, to.getLang());
  }

  /**
   * Convert the record to XML and normalize the hierarchy (see
   * {@link RdfXmlHierarchyNormalization}).
   *
   * @param content The content to convert and normalize.
   * @param from    The representation of the source.
   * @param baseUrl The base URL for the content. Can be <code>null</code>, in which case an
   *                exception is thrown if relative URLs are encountered.
   * @return The content in the XML representation.
   * @throws ComplianceException When input was found to be noncompliant.
   */
  public static String convertToXmlAndNormalizeHierarchy(String content, RdfRepresentation from,
      String baseUrl) throws ComplianceException {
    return convertToXmlAndNormalizeHierarchy(new ByteArrayInputStream(content.getBytes()), from,
        baseUrl);
  }

  /**
   * Convert the record to XML and normalize the hierarchy (see
   * {@link RdfXmlHierarchyNormalization}).
   *
   * @param content The content to convert and normalize as an input stream.
   * @param from    The representation of the source.
   * @param baseUrl The base URL for the content. Can be <code>null</code>, in which case an
   *                exception is thrown if relative URLs are encountered.
   * @return The content in the XML representation.
   * @throws ComplianceException When input was found to be noncompliant.
  */
  public static String convertToXmlAndNormalizeHierarchy(InputStream content,
      RdfRepresentation from, String baseUrl) throws ComplianceException {
    final ByteArrayOutputStream convertedRecord = new ByteArrayOutputStream();
    convertRdf(content, from, RdfRepresentation.XML, baseUrl, convertedRecord);
    try {
      return RdfXmlHierarchyNormalization
          .normalizeHierarchy(convertedRecord.toString(Charset.defaultCharset()));
    } catch (ComplianceException e) {
      throw new IllegalStateException("Unexpected issue with hierarchy normalization.", e);
    }
  }

  /**
   * Convert the record to XML and normalize the hierarchy (see
   * {@link RdfXmlHierarchyNormalization}).
   *
   * @param content The content to convert and normalize as an input stream.
   * @param from    The representation of the source.
   * @param baseUrl The base URL for the content. Can be <code>null</code>, in which case an
   *                exception is thrown if relative URLs are encountered.
   * @param result  The stream where the result is to be written. Content will be written in the
   *                default character encoding.
   * @throws ComplianceException When input was found to be noncompliant.
   * @throws IOException When something went wrong while writing to the provided output stream.
   */
  public static void convertToXmlAndNormalizeHierarchy(InputStream content, RdfRepresentation from,
      String baseUrl, OutputStream result) throws ComplianceException, IOException {
    final String normalizedRecord = convertToXmlAndNormalizeHierarchy(content, from, baseUrl);
    result.write(normalizedRecord.getBytes());
  }
}
