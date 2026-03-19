package eu.europeana.metis.common.rdf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.jena.riot.Lang;

/**
 * Enum containing the supported representations.
 */
public enum RdfRepresentation {

  XML(Lang.RDFXML, Set.of("application/xml", "text/xml", "application/rdf+xml"), ".rdf"),
  TTL(Lang.TTL, Set.of("text/ttl"), ".ttl"),
  NT(Lang.NT, Set.of("application/n-triples"), ".nt");

  private final Lang lang;
  private final Set<String> mediaTypes;
  private final String fileExtension;

  RdfRepresentation(Lang lang, Set<String> mediaTypes, String fileExtension) {
    this.lang = lang;
    this.mediaTypes = mediaTypes;
    this.fileExtension = fileExtension;
  }

  /**
   * @return The Jena {@link Lang} equivalent for this representation.
   */
  public Lang getLang() {
    return lang;
  }

  /**
   * @return The media types (mime types) that are equivalent for this representation.
   */
  public Set<String> getMediaTypes() {
    return Collections.unmodifiableSet(mediaTypes);
  }

  /**
   * @return The file extension (with leading '.') for files in this representation.
   */
  public String getFileExtension() {
    return fileExtension;
  }

  /**
   * Return the representation for the given media type if it exists.
   *
   * @param mediaType The media type for which to get the representation.
   * @return The representation matching the media type, or <code>null</code> if none were found.
   */
  public static RdfRepresentation forMediaType(String mediaType) {
    final List<RdfRepresentation> matches = Arrays.stream(values())
        .filter(representation -> representation.mediaTypes.contains(mediaType)).toList();
    if (matches.isEmpty()) {
      return null;
    }
    if (matches.size() > 1) {
      throw new IllegalStateException("Multiple representation for media type " + mediaType);
    }
    return matches.getFirst();
  }
}
