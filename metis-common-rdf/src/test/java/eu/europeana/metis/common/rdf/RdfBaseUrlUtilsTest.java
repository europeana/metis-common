package eu.europeana.metis.common.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;

class RdfBaseUrlUtilsTest {

  @Test
  void undoResolutionAgainstDefaultBaseUrlTest() throws URISyntaxException {
    final URI defaultBaseUrl = new URI(RdfBaseUrlUtils.DEFAULT_BASE_URL);
    assertEquals("A", RdfBaseUrlUtils.undoResolutionAgainstDefaultBaseUrl(
        defaultBaseUrl.resolve("A").toString()));
    assertEquals("#A", RdfBaseUrlUtils.undoResolutionAgainstDefaultBaseUrl(
        defaultBaseUrl.resolve("#A").toString()));
    assertEquals("A/B", RdfBaseUrlUtils.undoResolutionAgainstDefaultBaseUrl(
        defaultBaseUrl.resolve("A/B").toString()));
    assertEquals("/A/B", RdfBaseUrlUtils.undoResolutionAgainstDefaultBaseUrl(
        defaultBaseUrl.resolve("/A/B").toString()));
    assertEquals("http://example.com/A/B",
        RdfBaseUrlUtils.undoResolutionAgainstDefaultBaseUrl("http://example.com/A/B"));
    assertNull(RdfBaseUrlUtils.undoResolutionAgainstDefaultBaseUrl(null));
  }

  @Test
  void replaceDefaultBaseUrlTest() throws URISyntaxException {
    final URI defaultBaseUrl = new URI(RdfBaseUrlUtils.DEFAULT_BASE_URL);
    final URI newDefaultUrl = new URI("http://a.com/path/");
    assertEquals("http://a.com/path/A", RdfBaseUrlUtils.replaceDefaultBaseUrl(
        defaultBaseUrl.resolve("A").toString(), newDefaultUrl));
    assertEquals("http://a.com/path/#A", RdfBaseUrlUtils.replaceDefaultBaseUrl(
        defaultBaseUrl.resolve("#A").toString(), newDefaultUrl));
    assertEquals("http://a.com/path/A/B", RdfBaseUrlUtils.replaceDefaultBaseUrl(
        defaultBaseUrl.resolve("A/B").toString(), newDefaultUrl));
    assertEquals("http://a.com/A/B", RdfBaseUrlUtils.replaceDefaultBaseUrl(
        defaultBaseUrl.resolve("/A/B").toString(), newDefaultUrl));
    assertNull(RdfBaseUrlUtils.replaceDefaultBaseUrl("http://example.com/A/B", newDefaultUrl));
    assertNull(RdfBaseUrlUtils.replaceDefaultBaseUrl((String) null, newDefaultUrl));
  }
}
