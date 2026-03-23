package eu.europeana.metis.common.rdf;

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
   * Detects whether {@link #DEFAULT_BASE_URL} occurs anywhere in the model. This is the same as
   * checking whether there are any subjects or objects in the model that are relative to the base
   * URL. Note that properties need to be defined fully and cannot have been relative URIs.
   *
   * @param model The model to check.
   * @return Whether the default base url is used.
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
      return StreamSupport.stream(iterable.spliterator(), false)
          .filter(RDFNode::isResource).map(RDFNode::asResource)
          .filter(resource -> !resource.isAnon())
          .map(Resource::getURI).anyMatch(id -> id.startsWith(DEFAULT_BASE_DOMAIN));
    } finally {
      nodes.close();
    }
  }
}
