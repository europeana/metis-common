package eu.europeana.metis.common.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RdfXmlHierarchyNormalizationTest {

  private static final String GOOD_DATA_INPUT = """
      <n.1:RDF
          xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
          xmlns:n.2="http://example.com/2/"
          xmlns:n.3="http://example.com/3/">
        <n.2:Resource n.1:about="http://example.com/items/5">
          <n.3:label xml:lang="nl">Resource 6 label 1</n.3:label>
          <n.3:label xml:lang="en">Resource 6 label 2</n.3:label>
        </n.2:Resource>
        <n.2:Resource n.1:about="http://example.com/items/1" xmlns:n.a="http://example.com/A/">
          <n.2:property2A xmlns:n.b="http://example.com/B/" xmlns:n.1="http://example.com/1/">
            <n.2:Resource n.1:about="http://example.com/items/2" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <n.a:test>TEST A</n.a:test>
              <n.b:test>TEST B</n.b:test>
              <n.2:property2B xmlns:n.a="http://example.com/Aprime/">
                <n.2:Resource n.1:about="http://example.com/items/3" xmlns:n.c="http://example.com/C/">
                 <n.a:test>TEST A PRIME</n.a:test>
                 <n.b:test>TEST B</n.b:test>
                 <n.c:test>TEST C</n.c:test>
                </n.2:Resource>
              </n.2:property2B>
              <n.2:property2C>
                <n.2:Resource n.1:about="http://example.com/items/4">
                  <n.2:property2A n.1:resource="http://example.com/items/2"/>
                </n.2:Resource>
              </n.2:property2C>
              <n.2:label>label without language</n.2:label>
              <n.2:label>another label without language</n.2:label>
              <n.2:property2D n.1:resource="http://example.com/items/1"/>
              <n.2:property2E n.1:resource="http://example.com/items/4"/>
              <n.2:property2F n.1:resource="http://example.com/items/5"/>
            </n.2:Resource>
          </n.2:property2A>
        </n.2:Resource>
      </n.1:RDF>""";

  private static final String GOOD_DATA_OUTPUT = """
      <?xml version="1.0" ?>
      <n.1:RDF xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:n.2="http://example.com/2/" xmlns:n.3="http://example.com/3/">
        <n.2:Resource n.1:about="http://example.com/items/5">
          <n.3:label xml:lang="nl">Resource 6 label 1</n.3:label>
          <n.3:label xml:lang="en">Resource 6 label 2</n.3:label>
        </n.2:Resource>
        <n.2:Resource xmlns:n.a="http://example.com/A/" n.1:about="http://example.com/items/1">
          <n.2:property2A xmlns:rdf0="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:n.b="http://example.com/B/" xmlns:n.1="http://example.com/1/" rdf0:resource="http://example.com/items/2"></n.2:property2A>
        </n.2:Resource>
        <n.2:Resource xmlns:n.a="http://example.com/A/" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdf0="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:n.b="http://example.com/B/" n.1:about="http://example.com/items/2">
          <n.a:test>TEST A</n.a:test>
          <n.b:test>TEST B</n.b:test>
          <n.2:property2B xmlns:n.a="http://example.com/Aprime/" n.1:resource="http://example.com/items/3"></n.2:property2B>
          <n.2:property2C n.1:resource="http://example.com/items/4"></n.2:property2C>
          <n.2:label>label without language</n.2:label>
          <n.2:label>another label without language</n.2:label>
          <n.2:property2D n.1:resource="http://example.com/items/1"></n.2:property2D>
          <n.2:property2E n.1:resource="http://example.com/items/4"></n.2:property2E>
          <n.2:property2F n.1:resource="http://example.com/items/5"></n.2:property2F>
        </n.2:Resource>
        <n.2:Resource xmlns:n.a="http://example.com/Aprime/" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdf0="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:n.c="http://example.com/C/" xmlns:n.b="http://example.com/B/" n.1:about="http://example.com/items/3">
          <n.a:test>TEST A PRIME</n.a:test>
          <n.b:test>TEST B</n.b:test>
          <n.c:test>TEST C</n.c:test>
        </n.2:Resource>
        <n.2:Resource xmlns:n.a="http://example.com/A/" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdf0="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:n.b="http://example.com/B/" n.1:about="http://example.com/items/4">
          <n.2:property2A n.1:resource="http://example.com/items/2"></n.2:property2A>
        </n.2:Resource>
      </n.1:RDF>""";

  private static final String VERY_SIMPLE_DATA_INPUT_AND_OUTPUT = """
      <?xml version="1.0"?>
      <n.1:RDF xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#">TEST</n.1:RDF>""";

  private static final String FIRST_PREFIX_CHOICES_ALREADY_USED_INPUT = """
      <?xml version="1.0"?>
      <rdf0:Root xmlns:rdf0="http://example.com/rdf0">
        <rdf0:Resource>
          <rdf1:property xmlns:rdf1="http://example.com/rdf1">
            <rdf0:Resource n.1:about="http://example.com/items/A" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf1:property xmlns:n.1="http://example.com/1">
                <rdf0:Resource n.1:about="http://example.com/items/B" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf0:label>TEST</rdf0:label>
                </rdf0:Resource>
              </rdf1:property>
            </rdf0:Resource>
          </rdf1:property>
        </rdf0:Resource>
      </rdf0:Root>""";

  private static final String FIRST_PREFIX_CHOICES_ALREADY_USED_OUTPUT = """
      <?xml version="1.0"?>
      <rdf0:Root xmlns:rdf0="http://example.com/rdf0">
        <rdf0:Resource>
          <rdf1:property xmlns:rdf2="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdf1="http://example.com/rdf1" rdf2:resource="http://example.com/items/A"></rdf1:property>
        </rdf0:Resource>
        <rdf0:Resource xmlns:rdf1="http://example.com/rdf1" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdf2="http://www.w3.org/1999/02/22-rdf-syntax-ns#" n.1:about="http://example.com/items/A">
          <rdf1:property xmlns:n.1="http://example.com/1" rdf2:resource="http://example.com/items/B"></rdf1:property>
        </rdf0:Resource>
        <rdf0:Resource xmlns:rdf1="http://example.com/rdf1" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdf2="http://www.w3.org/1999/02/22-rdf-syntax-ns#" n.1:about="http://example.com/items/B">
          <rdf0:label>TEST</rdf0:label>
        </rdf0:Resource>
      </rdf0:Root>""";

  private static final String PROPERTY_WITH_TWO_CHILDREN_INPUT = """
      <?xml version="1.0"?>
      <n.1:RDF xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:n.2="http://example.com/2/">
        <n.2:Resource>
          <n.2:property>
            <n.2:Resource>
              <n.2:label>Resource A</n.2:label>
            </n.2:Resource>
            <n.2:Resource n.1:about="http://example.com/items/B">
              <n.2:label>Resource B</n.2:label>
            </n.2:Resource>
          </n.2:property2A>
        </n.1:Resource>
      </n.1:RDF>""";

  private static final String PROPERTY_WITH_CONFLICTING_REFERENCE_INPUT = """
      <?xml version="1.0"?>
      <n.1:RDF xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:n.2="http://example.com/2/">
        <n.2:Resource>
          <n.2:property n.1:resource="http://example.com/items/A">
            <n.2:Resource n.1:about="http://example.com/items/B">
              <n.2:label>Resource B</n.2:label>
            </n.2:Resource>
          </n.2:property2A>
        </n.1:Resource>
      </n.1:RDF>""";

  private static final String DATA_WITH_DEFAULT_NAMESPACES_INPUT = """
      <RDF
          xmlns="http://example.com/default_A/"
          xmlns:n.2="http://example.com/2/"
          xmlns:n.3="http://example.com/3/">
        <Resource>
          <property2A xmlns="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <Resource n.1:about="http://example.com/items/2" xmlns="http://example.com/default_A/" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <n.2:property2B>
                <n.2:Resource n.1:about="http://example.com/items/4">
                  <property2A n.1:resource="http://example.com/items/2"/>
                </n.2:Resource>
              </n.2:property2B>
            </Resource>
          </property2A>
        </Resource>
      </RDF>""";

  private static final String DATA_WITH_DEFAULT_NAMESPACES_OUTPUT = """
      <?xml version="1.0" ?>
      <RDF xmlns="http://example.com/default_A/" xmlns:n.2="http://example.com/2/" xmlns:n.3="http://example.com/3/">
        <Resource>
          <property2A xmlns:rdf0="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://www.w3.org/1999/02/22-rdf-syntax-ns#" rdf0:resource="http://example.com/items/2"></property2A>
        </Resource>
        <Resource xmlns="http://example.com/default_A/" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdf0="http://www.w3.org/1999/02/22-rdf-syntax-ns#" n.1:about="http://example.com/items/2">
          <n.2:property2B n.1:resource="http://example.com/items/4"></n.2:property2B>
        </Resource>
        <n.2:Resource xmlns="http://example.com/default_A/" xmlns:n.1="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdf0="http://www.w3.org/1999/02/22-rdf-syntax-ns#" n.1:about="http://example.com/items/4">
          <property2A n.1:resource="http://example.com/items/2"></property2A>
        </n.2:Resource>
      </RDF>""";

  @Test
  void testData() throws ComplianceException {
    assertEquals(GOOD_DATA_OUTPUT, RdfXmlHierarchyNormalization.normalizeHierarchy(GOOD_DATA_INPUT));
    assertEquals(VERY_SIMPLE_DATA_INPUT_AND_OUTPUT,
        RdfXmlHierarchyNormalization.normalizeHierarchy(VERY_SIMPLE_DATA_INPUT_AND_OUTPUT));
    assertEquals(FIRST_PREFIX_CHOICES_ALREADY_USED_OUTPUT,
        RdfXmlHierarchyNormalization.normalizeHierarchy(FIRST_PREFIX_CHOICES_ALREADY_USED_INPUT));
    assertThrows(RdfComplianceException.class,
        () -> RdfXmlHierarchyNormalization.normalizeHierarchy(PROPERTY_WITH_TWO_CHILDREN_INPUT));
    assertThrows(RdfComplianceException.class,
        () -> RdfXmlHierarchyNormalization.normalizeHierarchy(
            PROPERTY_WITH_CONFLICTING_REFERENCE_INPUT));
    assertEquals(DATA_WITH_DEFAULT_NAMESPACES_OUTPUT,
        RdfXmlHierarchyNormalization.normalizeHierarchy(DATA_WITH_DEFAULT_NAMESPACES_INPUT));
  }
}
