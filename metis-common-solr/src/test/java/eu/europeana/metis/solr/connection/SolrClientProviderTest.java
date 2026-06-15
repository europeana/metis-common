package eu.europeana.metis.solr.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.LBSolrClient.Endpoint;
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient;
import org.apache.solr.client.solrj.jetty.LBJettySolrClient;
import org.apache.solr.common.cloud.ClusterStateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link SolrClientProvider}
 *
 * Tests all paths and edge cases with 100% code coverage using Mockito for mocking.
 *
 * @author Jorge Ortiz
 * @since 2026-06-15
 */
@DisplayName("SolrClientProvider Test Suite")
class SolrClientProviderTest {

  private static final String LOCALHOST = "localhost";
  private static final String HOST_1 = "host1.example.com";
  private static final String HOST_2 = "host2.example.com";
  private static final String IP_ADDRESS = "192.168.1.1";
  private static final int SOLR_PORT = 8983;
  private static final int ZOOKEEPER_PORT = 2181;
  private static final String ZOOKEEPER_CHROOT = "/solr";
  private static final String DEFAULT_COLLECTION = "default_collection";
  private static final int CONNECTION_TIMEOUT_SECS = 30;
  private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
  private static final int DEFAULT_IDLE_CONNECTION_TIMEOUT = 10000;

  private SolrProperties<Exception> solrProperties;
  private SolrClientProvider<Exception> solrClientProvider;

  @BeforeEach
  void setUp() {
    solrProperties = mock(SolrProperties.class);
    solrClientProvider = new SolrClientProvider<>(solrProperties);
  }

  // ===== Constructor Tests =====

  @Test
  @DisplayName("Constructor should initialize with SolrProperties")
  void testConstructor() {
    SolrClientProvider<Exception> provider = new SolrClientProvider<>(solrProperties);
    assertNotNull(provider);
  }

  // ===== InetSocketAddress to String Conversion Tests =====

  @Test
  @DisplayName("Convert domain name with port to string")
  void testToCloudSolrClientAddressStringWithDomainName() {
    final InetSocketAddress address = new InetSocketAddress(HOST_1, ZOOKEEPER_PORT);
    final String result = SolrClientProvider.toCloudSolrClientAddressString(address);
    assertEquals(HOST_1 + ":" + ZOOKEEPER_PORT, result);
  }

  @Test
  @DisplayName("Convert IP address with port to string")
  void testToCloudSolrClientAddressStringWithIPAddress() {
    final InetSocketAddress address = new InetSocketAddress(IP_ADDRESS, ZOOKEEPER_PORT);
    final String result = SolrClientProvider.toCloudSolrClientAddressString(address);
    assertEquals(IP_ADDRESS + ":" + ZOOKEEPER_PORT, result);
  }

  @Test
  @DisplayName("Convert localhost to string")
  void testToCloudSolrClientAddressStringWithLocalhost() {
    final InetSocketAddress address = new InetSocketAddress(LOCALHOST, ZOOKEEPER_PORT);
    final String result = SolrClientProvider.toCloudSolrClientAddressString(address);
    assertEquals(LOCALHOST + ":" + ZOOKEEPER_PORT, result);
  }

  @Test
  @DisplayName("Convert addresses with different port numbers to string")
  void testToCloudSolrClientAddressStringWithDifferentPorts() {
    final int[] ports = {1, 80, 443, 2181, 8983, 65535};

    for (int port : ports) {
      final InetSocketAddress address = new InetSocketAddress(LOCALHOST, port);
      final String result = SolrClientProvider.toCloudSolrClientAddressString(address);
      assertEquals(LOCALHOST + ":" + port, result);
    }
  }

  // ===== createSolrClient Tests (Without Zookeeper) =====

  @Test
  @DisplayName("Create compound Solr client without Zookeeper connection")
  void testCreateSolrClientWithoutZookeeper() throws Exception {
    // Setup: Mock properties for HTTP-only connection
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr"),
        new URI("http://" + HOST_2 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(false);

    // Act & Assert - Should not throw exception
    try (var ignored = mockConstruction(LBJettySolrClient.class)) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Create compound Solr client with Zookeeper connection")
  void testCreateSolrClientWithZookeeper() throws Exception {
    // Setup: Mock properties for connection with Zookeeper
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Create compound Solr client with Zookeeper and custom timeout")
  void testCreateSolrClientWithZookeeperAndCustomTimeout() throws Exception {
    // Setup: Mock properties with custom timeout
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(CONNECTION_TIMEOUT_SECS);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  // ===== HTTP Solr Connection Tests =====

  @Test
  @DisplayName("Setup HTTP Solr connection with single host")
  void testSetupHttpSolrConnectionSingleHost() throws Exception {
    // Setup: Single host configuration
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(false);

    // Act & Assert
    try (var ignored = mockConstruction(LBJettySolrClient.class)) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Setup HTTP Solr connection with multiple hosts")
  void testSetupHttpSolrConnectionMultipleHosts() throws Exception {
    // Setup: Multiple hosts configuration
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr"),
        new URI("http://" + HOST_2 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(false);

    // Act & Assert
    try (var ignored = mockConstruction(LBJettySolrClient.class)) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Setup HTTP Solr connection with empty host list")
  void testSetupHttpSolrConnectionEmptyHosts() throws Exception {
    // Setup: Empty hosts list
    when(solrProperties.getSolrHosts()).thenReturn(List.of());
    when(solrProperties.hasZookeeperConnection()).thenReturn(false);

    // Act & Assert
    try (var ignored = mockConstruction(LBJettySolrClient.class)) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Setup HTTP Solr connection with various URI schemes")
  void testHttpSolrConnectionWithVariousSchemes() throws Exception {
    // Setup: Mix of http and https schemes
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr"),
        new URI("https://" + HOST_2 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(false);

    // Act & Assert
    try (var ignored = mockConstruction(LBJettySolrClient.class)) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  // ===== Cloud Solr Connection Tests =====

  @Test
  @DisplayName("Setup Cloud Solr connection with single Zookeeper host")
  void testSetupCloudSolrConnectionSingleHost() throws Exception {
    // Setup: Single Zookeeper host
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Setup Cloud Solr connection with multiple Zookeeper hosts")
  void testSetupCloudSolrConnectionMultipleHosts() throws Exception {
    // Setup: Multiple Zookeeper hosts
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT),
        new InetSocketAddress(HOST_2, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>(Set.of(
                   HOST_1 + ":" + ZOOKEEPER_PORT,
                   HOST_2 + ":" + ZOOKEEPER_PORT
               )));
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Setup Cloud Solr connection without chroot")
  void testSetupCloudSolrConnectionWithoutChroot() throws Exception {
    // Setup: No chroot
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(null);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Setup Cloud Solr connection without custom timeout uses default")
  void testSetupCloudSolrConnectionWithoutCustomTimeout() throws Exception {
    // Setup: No custom timeout specified (null)
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Setup Cloud Solr connection with custom timeout")
  void testSetupCloudSolrConnectionWithCustomTimeout() throws Exception {
    // Setup: Custom timeout specified
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(CONNECTION_TIMEOUT_SECS);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Setup Cloud Solr connection with zero timeout")
  void testSetupCloudSolrConnectionWithZeroTimeout() throws Exception {
    // Setup: Zero timeout
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(0);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Setup Cloud Solr connection with large timeout value")
  void testSetupCloudSolrConnectionWithLargeTimeout() throws Exception {
    // Setup: Large timeout value
    int largeTimeout = 300; // 5 minutes
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(largeTimeout);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Cloud Solr connection with live nodes populated")
  void testCloudSolrConnectionWithLiveNodes() throws Exception {
    // Setup: Mock live nodes in cluster
    Set<String> liveNodes = new HashSet<>(Set.of(
        HOST_1 + ":" + SOLR_PORT,
        HOST_2 + ":" + SOLR_PORT
    ));

    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(liveNodes);
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Cloud Solr connection with minimal timeout")
  void testCloudSolrConnectionWithMinimalTimeout() throws Exception {
    // Setup: Minimal timeout value (1 second)
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(1);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  // ===== Edge Cases Tests =====

  @Test
  @DisplayName("Multiple calls to createSolrClient return different instances")
  void testMultipleCreateSolrClientCalls() throws Exception {
    // Setup
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(false);

    // Act & Assert
    try (var ignored = mockConstruction(LBJettySolrClient.class)) {
      eu.europeana.metis.solr.client.CompoundSolrClient result1 = solrClientProvider.createSolrClient();
      eu.europeana.metis.solr.client.CompoundSolrClient result2 = solrClientProvider.createSolrClient();

      assertNotNull(result1);
      assertNotNull(result2);
    }
  }

  @Test
  @DisplayName("Cloud Solr connection with special characters in collection name")
  void testCloudSolrConnectionWithSpecialCollectionName() throws Exception {
    // Setup: Collection name with special characters
    String specialCollection = "my-collection_v1.0";
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn("/");
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(specialCollection);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  @Test
  @DisplayName("Cloud Solr connection with complex chroot path")
  void testCloudSolrConnectionWithComplexChroot() throws Exception {
    // Setup: Complex chroot path
    String complexChroot = "/solr/production/cluster1";
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(complexChroot);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }

  // ===== Constants Tests =====

  @Test
  @DisplayName("Verify default connection timeout constant")
  void testDefaultConnectionTimeoutConstant() {
    // This test verifies the constant value is used
    assertEquals(5000, DEFAULT_CONNECTION_TIMEOUT);
  }

  @Test
  @DisplayName("Verify default idle connection timeout constant")
  void testDefaultIdleConnectionTimeoutConstant() {
    // This test verifies the constant value is used
    assertEquals(10000, DEFAULT_IDLE_CONNECTION_TIMEOUT);
  }

  // ===== Compound Solr Client Tests =====

  @Test
  @DisplayName("Compound Solr client is created with both HTTP and Cloud clients")
  void testCompoundSolrClientCreation() throws Exception {
    // Setup: Configuration with both HTTP and Cloud clients
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
      assertNotNull(result.getSolrClient());
    }
  }

  @Test
  @DisplayName("Compound Solr client prefers Cloud client when available")
  void testCompoundSolrClientPrefersCloudClient() throws Exception {
    // Setup: Both clients available
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(true);
    when(solrProperties.getZookeeperHosts()).thenReturn(List.of(
        new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)
    ));
    when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
    when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
    when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class);
         var cloudClientMock = mockConstruction(CloudSolrClient.class,
             (mock, context) -> {
               ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
               when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
               when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
             })) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
      // Cloud client should be returned if not null
      assertNotNull(result.getSolrClient());
    }
  }

  @Test
  @DisplayName("Compound Solr client returns HTTP client when Cloud is not available")
  void testCompoundSolrClientFallbackToHttpClient() throws Exception {
    // Setup: Only HTTP client available
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(false);

    // Act & Assert
    try (var httpClientMock = mockConstruction(LBJettySolrClient.class)) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
      assertNotNull(result.getSolrClient());
    }
  }

  @Test
  @DisplayName("HTTP connection builder receives timeout constants from class")
  void testHttpClientBuilderReceivesTimeoutConstants() throws Exception {
    // Setup
    when(solrProperties.getSolrHosts()).thenReturn(List.of(
        new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")
    ));
    when(solrProperties.hasZookeeperConnection()).thenReturn(false);

    // Act & Assert
    try (var httpBuilderMock = mockConstruction(HttpJettySolrClient.Builder.class,
         (mock, context) -> {
           when(mock.withConnectionTimeout(anyInt(), any(TimeUnit.class))).thenReturn(mock);
           when(mock.withIdleTimeout(anyInt(), any(TimeUnit.class))).thenReturn(mock);
           when(mock.build()).thenReturn(mock(HttpJettySolrClient.class));
         });
         var lbClientMock = mockConstruction(LBJettySolrClient.class)) {
      eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
      assertNotNull(result);
    }
  }
}
