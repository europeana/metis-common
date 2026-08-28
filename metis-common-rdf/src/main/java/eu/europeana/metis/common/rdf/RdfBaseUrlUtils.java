package eu.europeana.metis.common.rdf;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * This class contains utility methods around the default base domain. When loading a model into
 * Jena, a base URL should be set to handle relative URLs in the data. This is necessary to avoid
 * unexpected behavior, such as using the file path name on the machine where this code happens to
 * be running as base URL. This class provides a default base URL as well as functionality to
 * check for its use and to retrieve the original relative URL.
 */
public final class RdfBaseUrlUtils {

  /**
   * This is the default base domain, to be used internally only. See {@link #DEFAULT_BASE_URL} for
   * a full explanation.
   */
  private static final String DEFAULT_BASE_DOMAIN =
      "http://3a051336-f671-4e94-90db-45d3432181fb.example.com/";

  /**
   * <p>In case no base URL is known for data to be loaded into Jena, this one may be used. It will
   * avoid Jena making assumptions in case of relative URLs (e.g., using the file path name on the
   * machine where this code happens to be running).
   * </p>
   * <p>This URL is designed specifically to work for this purpose. The domain name (*.example.com)
   * is reserved and should never be used. Also, the UUID that is the first segment of the domain
   * name should be unique. As such, this URL should never occur in real data. Also, as there is a
   * path component after the domain name (also a UUID that does not occur in real data). This will
   * serve to distinguish two types of relative URLs: those relative to the domain (i.e., starting
   * with "/") vs. those relative to the path.
   * </p>
   */
  public static final String DEFAULT_BASE_URL = DEFAULT_BASE_DOMAIN +
      "7e894f24-c379-4cd8-9698-902d3f279732/";

  private RdfBaseUrlUtils() {
  }

  /**
   * This method undoes the resolution of relative URLs against {@link #DEFAULT_BASE_URL} that is
   * done when loading the data into Jena with {@link #DEFAULT_BASE_URL} as the base URL. If this
   * method is called for a URL that is not relative to {@link #DEFAULT_BASE_URL}, the url is
   * returned unchanged. Implementation note: the functionality <code>URI.relativize()</code>
   * does not work with relative URLs starting with '/' (i.e., relative to the domain).
   *
   * @param url The URL for which to undo resolution against {@link #DEFAULT_BASE_URL}.
   * @return The equivalent URL no longer relative to {@link #DEFAULT_BASE_URL}.
   */
  public static String undoResolutionAgainstDefaultBaseUrl(String url) {
    final String result;
    if (url == null || !url.startsWith(DEFAULT_BASE_DOMAIN)) {
      // URL is not relative to the default base domain. Return unchanged.
      result = url;
    } else if (url.startsWith(DEFAULT_BASE_URL)) {
      // URL is relative to the full base URL. So it was a relative URL not starting with '/'.
      result = url.substring(DEFAULT_BASE_URL.length());
    } else {
      // URL is relative to the base domain, but not the full base URL. So it was a relative URL
      // starting with '/'.
      result = "/" + url.substring(DEFAULT_BASE_DOMAIN.length());
    }
    return result;
  }

  /**
   * Checks whether the url is relative to the default base URL. This is the same as checking
   * whether the url starts with {@link #DEFAULT_BASE_DOMAIN}.
   *
   * @param url The url to check. Cannot be <code>null</code>.
   * @return Whether the base URL is
   */
  public static boolean isRelativeToDefaultBaseUrl(String url) {
    return url.startsWith(DEFAULT_BASE_DOMAIN);
  }

  /**
   * Checks whether the RDF node is (i.e., has an ID that is) relative to the default base URL.
   *
   * @param node The node to check.
   * @return Whether the node is relative to the default url.
   */
  public static boolean containsDefaultBaseUrl(RDFNode node) {
    return anyResourceHasIdRelativeToBaseUrl(Stream.of(node));
  }

  /**
   * Checks whether any resource in the model is (i.e., has an ID that is) relative to the
   * default base URL. Note that we check all subjects and resource referencing objects. We do not
   * look at literals. Also, the property itself could never have been a relative URL.
   *
   * @param model The model to check.
   * @return Whether any resource in the model is relative to the default url.
   */
  public static boolean containsDefaultBaseUrl(Model model) {
    return anyResourceHasIdRelativeToBaseUrl(model.listSubjects()) ||
        anyResourceHasIdRelativeToBaseUrl(model.listObjects());
  }

  /**
   * Returns whether any of the nodes has an ID (URI) relative to the base URL. We in fact check
   * against the base domain, as a relative URL starting with '/' will not contain the path
   * component of {@link #DEFAULT_BASE_URL}.
   *
   * @param nodes Iterator with nodes to check.
   * @param <T>   The type of the nodes in the iterator.
   * @return Whether any of them have an ID relative to the base URL.
   */
  private static <T extends RDFNode> boolean anyResourceHasIdRelativeToBaseUrl(
      ExtendedIterator<T> nodes) {
    try {
      final Iterable<T> iterable = () -> nodes;
      return anyResourceHasIdRelativeToBaseUrl(StreamSupport.stream(iterable.spliterator(), false));
    } finally {
      nodes.close();
    }
  }

  /**
   * Returns whether any of the nodes has an ID (URI) relative to the base URL. We in fact check
   * against the base domain, as a relative URL starting with '/' will not contain the path
   * component of {@link #DEFAULT_BASE_URL}.
   *
   * @param nodes Stream with nodes to check.
   * @param <T>   The type of the nodes in the stream.
   * @return Whether any of them have an ID relative to the base URL.
   */
  private static <T extends RDFNode> boolean anyResourceHasIdRelativeToBaseUrl(Stream<T> nodes) {
    return nodes.map(RdfBaseUrlUtils::toUri).filter(Objects::nonNull)
        .anyMatch(RdfBaseUrlUtils::isRelativeToDefaultBaseUrl);
  }

  /**
   * Convert an RDF node to a resource identifier if the node represents a non-anonymous resource
   * that is relative.
   *
   * @param node The node to convert.
   * @return The resource identifier, or null if the node is not a non-anonymous resource.
   */
  private static String toUri(RDFNode node) {
    return Optional.of(node).filter(RDFNode::isResource).map(RDFNode::asResource)
        .filter(resource -> !resource.isAnon()).map(Resource::getURI).orElse(null);
  }

  /**
   * Replace the default base URL with a new base URL. Returns null if the given URI is not relative
   * to the default base URL.
   *
   * @param uri        The URI to convert. Can be null (in which case null is returned).
   * @param newBaseUrl The new base URL. Must be an absolute non-null URI.
   * @return The new resource identifier, or null if the node required no changes.
   */
  public static String replaceDefaultBaseUrl(String uri, URI newBaseUrl) {
    return Optional.ofNullable(uri)
        .filter(RdfBaseUrlUtils::isRelativeToDefaultBaseUrl)
        .map(RdfBaseUrlUtils::undoResolutionAgainstDefaultBaseUrl)
        .map(newBaseUrl::resolve).map(URI::toString).orElse(null);
  }

  /**
   * Replace the default base URL with a new base URL. Returns null if the node does not need
   * adjustments. Specifically, if the node is not a non-anonymous resource, or if the identifier is
   * not relative to the default base URL, we return null.
   *
   * @param node       The node to convert.
   * @param newBaseUrl The new base URL. Must be an absolute non-null URI.
   * @return The new resource identifier, or null if the node required no changes.
   */
  public static String replaceDefaultBaseUrl(RDFNode node, URI newBaseUrl) {
    return replaceDefaultBaseUrl(toUri(node), newBaseUrl);
  }
}
