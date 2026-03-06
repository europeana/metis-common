package eu.europeana.metis.common.rdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;

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
public final class RdfConversion {

  // This domain name is reserved and with the unique ID should never occur in the wild. Also, as
  // there is no path component after the domain name, this will serve to detect relative URLs
  // starting with / as well (i.e., URLs relative to the domain).
  static final String DEFAULT_BASE_URL = "http://7e894f24-c379-4cd8-9698-902d3f279732.example.com/";

  private RdfConversion() {
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
   */
  public static String convertRdf(String content, RdfRepresentation from, RdfRepresentation to,
      String baseUrl) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    convertRdf(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), from, to,
        baseUrl, outputStream);
    return outputStream.toString(StandardCharsets.UTF_8);
  }

  /**
   * Convert RDF content from one representation to another. Note that this conversion is performed
   * even if the two representations are identical. A call to this method can then serve to
   * verify the content and achieve more consistency (a kind of 'normalization').
   *
   * @param content The content as an input stream.
   * @param from    The representation of the source.
   * @param to      The required representation of the result.
   * @param baseUrl The base URL for the content. Can be <code>null</code>, in which case an
   *                exception is thrown if relative URLs are encountered.
   * @param result  The stream where the result is to be written.
   */
  public static void convertRdf(InputStream content, RdfRepresentation from, RdfRepresentation to,
      String baseUrl, OutputStream result) {

    // Read the data to a model.
    final String nonNullBaseUrl = Optional.ofNullable(baseUrl)
        .filter(value -> !value.isBlank()).orElse(DEFAULT_BASE_URL);
    final Model model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(model, content, nonNullBaseUrl, from.getLang());

    // Check if there were relative URLs without a provided base URL.
    if (DEFAULT_BASE_URL.equals(nonNullBaseUrl)) {
      final StmtIterator statements = model.listStatements();
      try {
        for (Statement statement : (Iterable<? extends Statement>) () -> statements) {
          if (statement.getSubject().toString().startsWith(DEFAULT_BASE_URL) ||
              statement.getPredicate().toString().startsWith(DEFAULT_BASE_URL) ||
              (statement.getObject().isResource() &&
                  statement.getObject().asResource().toString().startsWith(DEFAULT_BASE_URL))) {
            throw new IllegalArgumentException(
                "This data has relative URLs and a base URL should be provided to convert it.");
          }
        }
      } finally {
        statements.close();
      }
    }

    // Write to a String.
    RDFDataMgr.write(result, model, to.getLang());
  }
}
