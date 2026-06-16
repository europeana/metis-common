package eu.europeana.metis.solr.connection;

import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;
import org.apache.solr.client.solrj.impl.ClusterStateProvider;
import org.apache.solr.client.solrj.jetty.LBJettySolrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;


class SolrClientProviderTest {
    private static final String LOCALHOST = "localhost";
    private static final String HOST_1 = "host1.example.com";
    private static final String HOST_2 = "host2.example.com";
    private static final String IP_ADDRESS = "192.168.1.1";
    private static final int SOLR_PORT = 8983;
    private static final int ZOOKEEPER_PORT = 2181;
    private static final String ZOOKEEPER_CHROOT = "/solr";
    private static final String DEFAULT_COLLECTION = "default_collection";
    private static final Integer CONNECTION_TIMEOUT_SECS = 30;

    private SolrProperties<Exception> solrProperties;
    private SolrClientProvider<Exception> solrClientProvider;

    @BeforeEach
    void setUp() {
        solrProperties = mock(SolrProperties.class);
        solrClientProvider = new SolrClientProvider<>(solrProperties);
    }

    @Test
    void testConstructor() {
        SolrClientProvider<Exception> provider = new SolrClientProvider<>(solrProperties);
        assertNotNull(provider);
    }

    @Test
    void testToCloudSolrClientAddressStringWithDomainName() {
        final InetSocketAddress address = new InetSocketAddress(HOST_1, ZOOKEEPER_PORT);
        final String result = SolrClientProvider.toCloudSolrClientAddressString(address);
        assertEquals(HOST_1 + ":" + ZOOKEEPER_PORT, result);
    }

    @Test
    void testToCloudSolrClientAddressStringWithIPAddress() {
        final InetSocketAddress address = new InetSocketAddress(IP_ADDRESS, ZOOKEEPER_PORT);
        final String result = SolrClientProvider.toCloudSolrClientAddressString(address);
        assertEquals(IP_ADDRESS + ":" + ZOOKEEPER_PORT, result);
    }

    @Test
    void testToCloudSolrClientAddressStringWithLocalhost() {
        final InetSocketAddress address = new InetSocketAddress(LOCALHOST, ZOOKEEPER_PORT);
        final String result = SolrClientProvider.toCloudSolrClientAddressString(address);
        assertEquals(LOCALHOST + ":" + ZOOKEEPER_PORT, result);
    }

    @Test
    void testToCloudSolrClientAddressStringWithDifferentPorts() {
        final int[] ports = {1, 80, 443, 2181, 8983, 65535};

        for (int port : ports) {
            final InetSocketAddress address = new InetSocketAddress(LOCALHOST, port);
            final String result = SolrClientProvider.toCloudSolrClientAddressString(address);
            assertEquals(LOCALHOST + ":" + port, result);
        }
    }

    @Test
    void testCreateSolrClientWithoutZookeeperAndMultipleHosts() throws Exception {
        // Setup: Mock properties for HTTP-only connection
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr"), new URI("http://" + HOST_2 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(false);

        try (var ignored = mockConstruction(LBJettySolrClient.class)) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @Test
    void testCreateSolrClientWithZookeeper() throws Exception {
        // Setup: Mock properties for connection with Zookeeper
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(true);
        when(solrProperties.getZookeeperHosts()).thenReturn(List.of(new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)));
        when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
        when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
        when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(CONNECTION_TIMEOUT_SECS);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class); var cloudClientMock = mockConstruction(CloudHttp2SolrClient.class, (mock, context) -> {
            ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
            when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
            when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
        })) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @Test
    void testSetupHttpSolrConnectionSingleHost() throws Exception {
        // Setup: Single host configuration
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(false);

        try (var ignored = mockConstruction(LBJettySolrClient.class)) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @Test
    void testSetupHttpSolrConnectionEmptyHosts() throws Exception {
        // Setup: Empty hosts list
        when(solrProperties.getSolrHosts()).thenReturn(List.of());
        when(solrProperties.hasZookeeperConnection()).thenReturn(false);

        try (var ignored = mockConstruction(LBJettySolrClient.class)) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }


    @Test
    void testSetupCloudSolrConnectionMultipleHosts() throws Exception {
        // Setup: Multiple Zookeeper hosts
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(true);
        when(solrProperties.getZookeeperHosts()).thenReturn(List.of(new InetSocketAddress(HOST_1, ZOOKEEPER_PORT), new InetSocketAddress(HOST_2, ZOOKEEPER_PORT)));
        when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
        when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
        when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class); var cloudClientMock = mockConstruction(CloudHttp2SolrClient.class, (mock, context) -> {
            ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
            when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
            when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>(Set.of(HOST_1 + ":" + ZOOKEEPER_PORT, HOST_2 + ":" + ZOOKEEPER_PORT)));
        })) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @Test
    void testSetupCloudSolrConnectionWithoutChroot() throws Exception {
        // Setup: No chroot
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(true);
        when(solrProperties.getZookeeperHosts()).thenReturn(List.of(new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)));
        when(solrProperties.getZookeeperChroot()).thenReturn(null);
        when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
        when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class); var cloudClientMock = mockConstruction(CloudHttp2SolrClient.class, (mock, context) -> {
            ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
            when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
            when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
        })) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @Test
    void testSetupCloudSolrConnectionWithoutCustomTimeout() throws Exception {
        // Setup: No custom timeout specified (null)
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(true);
        when(solrProperties.getZookeeperHosts()).thenReturn(List.of(new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)));
        when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
        when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
        when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class); var cloudClientMock = mockConstruction(CloudHttp2SolrClient.class, (mock, context) -> {
            ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
            when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
            when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
        })) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 30, 300})
    void testSetupCloudSolrConnectionWithCustomTimeout(Integer value) throws Exception {
        // Setup: Custom timeout specified
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(true);
        when(solrProperties.getZookeeperHosts()).thenReturn(List.of(new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)));
        when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
        when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
        when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(value);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class); var cloudClientMock = mockConstruction(CloudHttp2SolrClient.class, (mock, context) -> {
            ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
            when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
            when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
        })) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @Test
    void testCloudSolrConnectionWithLiveNodes() throws Exception {
        // Setup: Mock live nodes in cluster
        Set<String> liveNodes = new HashSet<>(Set.of(HOST_1 + ":" + SOLR_PORT, HOST_2 + ":" + SOLR_PORT));

        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(true);
        when(solrProperties.getZookeeperHosts()).thenReturn(List.of(new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)));
        when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
        when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
        when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class); var cloudClientMock = mockConstruction(CloudHttp2SolrClient.class, (mock, context) -> {
            ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
            when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
            when(mockStateProvider.getLiveNodes()).thenReturn(liveNodes);
        })) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @Test
    void testCloudSolrConnectionWithMinimalTimeout() throws Exception {
        // Setup: Minimal timeout value (1 second)
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(true);
        when(solrProperties.getZookeeperHosts()).thenReturn(List.of(new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)));
        when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
        when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
        when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(1);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class); var cloudClientMock = mockConstruction(CloudHttp2SolrClient.class, (mock, context) -> {
            ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
            when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
            when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
        })) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @Test
    void testMultipleCreateSolrClientCalls() throws Exception {
        // Setup
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(false);

        try (var ignored = mockConstruction(LBJettySolrClient.class)) {
            eu.europeana.metis.solr.client.CompoundSolrClient result1 = solrClientProvider.createSolrClient();
            eu.europeana.metis.solr.client.CompoundSolrClient result2 = solrClientProvider.createSolrClient();

            assertNotNull(result1);
            assertNotNull(result2);
        }
    }

    @Test
    void testCloudSolrConnectionWithSpecialCollectionName() throws Exception {
        // Setup: Collection name with special characters
        String specialCollection = "my-collection_v1.0";
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(true);
        when(solrProperties.getZookeeperHosts()).thenReturn(List.of(new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)));
        when(solrProperties.getZookeeperChroot()).thenReturn("/");
        when(solrProperties.getZookeeperDefaultCollection()).thenReturn(specialCollection);
        when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class); var cloudClientMock = mockConstruction(CloudHttp2SolrClient.class, (mock, context) -> {
            ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
            when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
            when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
        })) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @Test
    void testCloudSolrConnectionWithComplexChroot() throws Exception {
        // Setup: Complex chroot path
        String complexChroot = "/solr/production/cluster1";
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(true);
        when(solrProperties.getZookeeperHosts()).thenReturn(List.of(new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)));
        when(solrProperties.getZookeeperChroot()).thenReturn(complexChroot);
        when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
        when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class); var cloudClientMock = mockConstruction(CloudHttp2SolrClient.class, (mock, context) -> {
            ClusterStateProvider mockStateProvider = mock(ClusterStateProvider.class);
            when(mock.getClusterStateProvider()).thenReturn(mockStateProvider);
            when(mockStateProvider.getLiveNodes()).thenReturn(new HashSet<>());
        })) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
        }
    }

    @Test
    void testCompoundSolrClientCreation() throws Exception {
        // Setup: Configuration with both HTTP and Cloud clients
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(true);
        when(solrProperties.getZookeeperHosts()).thenReturn(List.of(new InetSocketAddress(HOST_1, ZOOKEEPER_PORT)));
        when(solrProperties.getZookeeperChroot()).thenReturn(ZOOKEEPER_CHROOT);
        when(solrProperties.getZookeeperDefaultCollection()).thenReturn(DEFAULT_COLLECTION);
        when(solrProperties.getZookeeperTimeoutInSecs()).thenReturn(null);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class); var cloudClientMock = mockConstruction(CloudHttp2SolrClient.class, (mock, context) -> {
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
    void testCompoundSolrClientFallbackToHttpClient() throws Exception {
        // Setup: Only HTTP client available
        when(solrProperties.getSolrHosts()).thenReturn(List.of(new URI("http://" + HOST_1 + ":" + SOLR_PORT + "/solr")));
        when(solrProperties.hasZookeeperConnection()).thenReturn(false);

        try (var httpClientMock = mockConstruction(LBJettySolrClient.class)) {
            eu.europeana.metis.solr.client.CompoundSolrClient result = solrClientProvider.createSolrClient();
            assertNotNull(result);
            assertNotNull(result.getSolrClient());
        }
    }

    @Test
    void testInetSocketAddressToString() {

        // Define input
        final String ipAddress = "8.8.8.8";
        final String domainName = "europeana.eu";
        final int port1 = 1234;
        final int port2 = 1234;

        // Test single domain name
        final InetSocketAddress domainInput = new InetSocketAddress(domainName, port1);
        final String domainOutput = domainName + ":" + port1;
        assertEquals(domainOutput, SolrClientProvider.toCloudSolrClientAddressString(domainInput));

        // Test single ip address name
        final InetSocketAddress ipInput = new InetSocketAddress(ipAddress, port2);
        final String ipOutput = ipAddress + ":" + port2;
        assertEquals(ipOutput, SolrClientProvider.toCloudSolrClientAddressString(ipInput));
    }
}