package eu.europeana.metis.common.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class RdfXmlHierarchyNormalizationTest {

  @Test
  void testData() throws ComplianceException, IOException {
    assertEquals(loadFile("good_data_output.rdf"),
        RdfXmlHierarchyNormalization.normalizeHierarchy(loadFile("good_data_input.rdf")));
    final String simpleFile = loadFile("simple_data_input_and_output.rdf");
    assertEquals(simpleFile, RdfXmlHierarchyNormalization.normalizeHierarchy(simpleFile));
    assertEquals(loadFile("prefix_candidate_already_used_output.rdf"),
        RdfXmlHierarchyNormalization.normalizeHierarchy(loadFile("prefix_candidate_already_used_input.rdf")));
    assertThrows(RdfComplianceException.class, () -> RdfXmlHierarchyNormalization
        .normalizeHierarchy(loadFile("property_with_two_children_input.rdf")));
    assertThrows(RdfComplianceException.class, () -> RdfXmlHierarchyNormalization
        .normalizeHierarchy(loadFile("property_with_conflicting_reference_input.rdf")));
    assertEquals(loadFile("data_with_default_namespaces_output.rdf"),
        RdfXmlHierarchyNormalization.normalizeHierarchy(loadFile("data_with_default_namespaces_input.rdf")));
  }

  private static String loadFile(String fileName) throws IOException {
    try (InputStream input = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("hierarchy_normalization/" + fileName)) {
      return input == null ? null : IOUtils.toString(input, StandardCharsets.UTF_8);
    }
  }
}
