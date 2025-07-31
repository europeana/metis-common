package eu.europeana.metis.solr.connection;

import static org.apache.commons.collections4.CollectionUtils.isEqualCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SolrPropertiesTest {

  public static final InetSocketAddress INET_SOCKET_ADDRESS_1 = new InetSocketAddress("127.0.0.1", 2181);
  public static final InetSocketAddress INET_SOCKET_ADDRESS_2 = new InetSocketAddress("127.0.0.2", 2182);
  public static final InetSocketAddress INET_SOCKET_ADDRESS_3 = new InetSocketAddress("127.0.0.3", 2183);
  private SolrProperties<Exception> solrProperties;

  @BeforeEach
  void setup() {
    solrProperties = new SolrProperties<>(Exception::new);
  }

  @AfterEach
  void teardown() {
    solrProperties = null;
  }

  @Test
  void setZookeeperHosts() throws Exception {
    final String[] zooKeeperHosts = new String[]{
        INET_SOCKET_ADDRESS_1.getHostString(),
        INET_SOCKET_ADDRESS_2.getHostString(),
        INET_SOCKET_ADDRESS_3.getHostString()};
    final int[] zooKeeperPorts = new int[]{
        INET_SOCKET_ADDRESS_1.getPort(),
        INET_SOCKET_ADDRESS_2.getPort(),
        INET_SOCKET_ADDRESS_3.getPort()};

    solrProperties.setZookeeperHosts(zooKeeperHosts, zooKeeperPorts);

    final List<InetSocketAddress> actualZookeeperHosts = solrProperties.getZookeeperHosts();
    List<InetSocketAddress> inetSocketAddresses = List.of(
        INET_SOCKET_ADDRESS_2, INET_SOCKET_ADDRESS_1, INET_SOCKET_ADDRESS_3);
    assertTrue(isEqualCollection(inetSocketAddresses, actualZookeeperHosts));
  }

  @Test
  void setZookeeperHosts_nullHosts_shouldThrowException() {
    final int[] ports = {INET_SOCKET_ADDRESS_1.getPort()};
    assertThrows(Exception.class, () -> solrProperties.setZookeeperHosts(null, ports));
  }

  @Test
  void setZookeeperHosts_nullPorts_shouldThrowException() {
    final String[] hosts = {INET_SOCKET_ADDRESS_1.getHostString()};
    assertThrows(Exception.class, () -> solrProperties.setZookeeperHosts(hosts, null));
  }

  @Test
  void addZookeeperHost() throws Exception {
    solrProperties.addZookeeperHost(INET_SOCKET_ADDRESS_1);
    final List<InetSocketAddress> actualZookeeperHosts = solrProperties.getZookeeperHosts();
    assertTrue(isEqualCollection(List.of(INET_SOCKET_ADDRESS_1), actualZookeeperHosts));
  }

  @Test
  void addZookeeperHostException() {
    assertThrows(Exception.class, () -> solrProperties.addZookeeperHost(null));
  }

  @Test
  void setZookeeperChroot() throws Exception {
    solrProperties.addZookeeperHost(INET_SOCKET_ADDRESS_1);
    solrProperties.setZookeeperChroot("/zookeeper");
    assertEquals("/zookeeper", solrProperties.getZookeeperChroot());
  }

  @Test
  void setZookeeperChrootNull() throws Exception {
    solrProperties.addZookeeperHost(INET_SOCKET_ADDRESS_1);
    solrProperties.setZookeeperChroot("");
    assertNull(solrProperties.getZookeeperChroot());
  }

  @Test
  void setZookeeperChrootException() throws Exception {
    solrProperties.addZookeeperHost(INET_SOCKET_ADDRESS_1);
    assertThrows(Exception.class, () -> solrProperties.setZookeeperChroot("root"));
  }

  @Test
  void getZookeeperChroot_withoutZkHost_shouldReturnNull() {
    assertNull(solrProperties.getZookeeperChroot());
  }

  @Test
  void setZookeeperDefaultCollection() throws Exception {
    solrProperties.addZookeeperHost(INET_SOCKET_ADDRESS_1);
    solrProperties.setZookeeperDefaultCollection("zookeeperCollection");
    assertEquals("zookeeperCollection", solrProperties.getZookeeperDefaultCollection());
  }

  @Test
  void setZookeeperDefaultCollectionNull() throws Exception {
    solrProperties.addZookeeperHost(INET_SOCKET_ADDRESS_1);
    assertThrows(Exception.class, () -> solrProperties.setZookeeperDefaultCollection(null));
  }

  @Test
  void getZookeeperDefaultCollection_withoutZkHost_shouldReturnNull() throws Exception {
    assertNull(solrProperties.getZookeeperDefaultCollection());
  }

  @Test
  void getZookeeperDefaultCollection_withoutNullZkHost_shouldThrowException() throws Exception {
    solrProperties.addZookeeperHost(INET_SOCKET_ADDRESS_1);
    assertThrows(Exception.class, () -> solrProperties.getZookeeperDefaultCollection());
  }

  @Test
  void setZookeeperTimeoutInSecs() throws Exception {
    solrProperties.addZookeeperHost(INET_SOCKET_ADDRESS_1);
    solrProperties.setZookeeperTimeoutInSecs(10);
    assertEquals(10, solrProperties.getZookeeperTimeoutInSecs());
  }

  @Test
  void setZookeeperTimeoutNegative() throws Exception {
    solrProperties.addZookeeperHost(INET_SOCKET_ADDRESS_1);
    solrProperties.setZookeeperTimeoutInSecs(-1);
    assertNull(solrProperties.getZookeeperTimeoutInSecs());
  }

  @Test
  void getZookeeperTimeoutInSecs_withoutZkConnection_shouldReturnNull() {
    assertNull(solrProperties.getZookeeperTimeoutInSecs());
  }

  @Test
  void addSolrHost() throws Exception {
    final URI uri = new URI("http://localhost:8983/solr");
    solrProperties.addSolrHost(uri);
    List<URI> actualSolrHosts = solrProperties.getSolrHosts();
    assertTrue(isEqualCollection(List.of(uri), actualSolrHosts));
  }

  @Test
  void getSolrHostsThrowsExceptionWhenEmpty() {
    assertThrows(Exception.class, () -> solrProperties.getSolrHosts());
  }

  @Test
  void hasZookeeperConnection() throws Exception {
    solrProperties.addZookeeperHost(INET_SOCKET_ADDRESS_1);
    assertTrue(solrProperties.hasZookeeperConnection());
  }

  @Test
  void hasZookeeperConnection_False_When_Empty() {
    assertFalse(solrProperties.hasZookeeperConnection());
  }
}