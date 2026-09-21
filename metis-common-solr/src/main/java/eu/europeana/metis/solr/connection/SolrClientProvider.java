package eu.europeana.metis.solr.connection;

import eu.europeana.metis.solr.client.CompoundSolrClient;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.impl.LBSolrClient.Endpoint;
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient;
import org.apache.solr.client.solrj.jetty.LBJettySolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class can set up and provide a Solr client given the Solr properties.
 *
 * @param <E> The type of exception thrown when the properties are not valid.
 */
public class SolrClientProvider<E extends Exception> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrClientProvider.class);
  private final SolrProperties<E> settings;

  /**
   * Constructor.
   *
   * @param properties The properties of the Mongo connection.
   */
  public SolrClientProvider(SolrProperties<E> properties) {
    this.settings = properties;
  }

  /**
   * Creates a Solr client from the properties. This method can be called multiple times and will return a different client each
   * time.
   *
   * @return A Solr client.
   * @throws E In case there is a problem with the supplied properties.
   */
  public CompoundSolrClient createSolrClient() throws E {
    final HttpConnection connection = setUpHttpSolrConnection();
    try (ClientCleanup cleanup = new ClientCleanup(
        new CompoundSolrClient(connection.client(), null, connection.transport()))) {
      final CloudSolrClient cloudSolrClient = settings.hasZookeeperConnection()
          ? setUpCloudSolrConnection(connection.client()) : null;
      final CompoundSolrClient result =
          new CompoundSolrClient(connection.client(), cloudSolrClient, connection.transport());
      cleanup.transferOwnership();
      return result;
    }
  }

  private record HttpConnection(SolrClient client, HttpJettySolrClient transport) {

  }

  private HttpConnection setUpHttpSolrConnection() throws E {
    final Endpoint[] solrHosts =
        settings.getSolrHosts().stream().map(host -> new Endpoint(host.toString())).toArray(Endpoint[]::new);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Connecting to Solr hosts: [{}]",
          String.join(", ", Arrays.stream(solrHosts).map(Endpoint::toString).toArray(String[]::new)));
    }

    if (settings.getSolrUseHttp1()) {
      return new HttpConnection(new HttpJdkSolrClient.Builder(solrHosts[0].getBaseUrl())
          .withConnectionTimeout(settings.getSolrClientConnectionTimeoutInSecs(), TimeUnit.SECONDS)
          .withIdleTimeout(settings.getSolrClientIdleConnectionTimeoutInSecs(), TimeUnit.SECONDS)
          .useHttp1_1(settings.getSolrUseHttp1())
          .build(), null);
    } else {
      HttpJettySolrClient baseClient = new HttpJettySolrClient.Builder()
          .withConnectionTimeout(settings.getSolrClientConnectionTimeoutInSecs(), TimeUnit.SECONDS)
          .withIdleTimeout(settings.getSolrClientIdleConnectionTimeoutInSecs(), TimeUnit.SECONDS)
          .build();
      try (ClientCleanup clientCleanup = new ClientCleanup(baseClient)) {
        final HttpConnection connection =
            new HttpConnection(new LBJettySolrClient.Builder(baseClient, solrHosts).build(), baseClient);
        clientCleanup.transferOwnership();
        return connection;
      }
    }
  }

  private CloudSolrClient setUpCloudSolrConnection(SolrClient solrClient) throws E {

    // Get information from settings
    final Set<String> hosts = settings.getZookeeperHosts().stream()
                                      .map(SolrClientProvider::toCloudSolrClientAddressString).collect(Collectors.toSet());
    final String chRoot = settings.getZookeeperChroot();
    final String defaultCollection = settings.getZookeeperDefaultCollection();
    final Integer connectionTimeoutInSecs = settings.getZookeeperTimeoutInSecs();

    // Configure connection builder
    final CloudSolrClient.Builder builder = new CloudSolrClient.Builder(List.copyOf(hosts), Optional.ofNullable(chRoot));
    // Set up Zookeeper connection
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          "Connecting to Zookeeper hosts: [{}] with chRoot [{}] and default connection [{}]. Connection time-out: {}.",
          String.join(", ", hosts), chRoot, defaultCollection,
          connectionTimeoutInSecs == null ? "default" : (connectionTimeoutInSecs + " seconds"));
    }
    if (connectionTimeoutInSecs != null) {
      final int timeoutInMillis = (int) Duration.ofSeconds(connectionTimeoutInSecs).toMillis();
      builder.withZkConnectTimeout(timeoutInMillis, TimeUnit.MILLISECONDS);
      builder.withZkClientTimeout(timeoutInMillis, TimeUnit.MILLISECONDS);
    }
    builder.withDefaultCollection(defaultCollection);
    if (settings.getSolrUseHttp1()) {
      builder.withHttpClient((HttpJdkSolrClient) solrClient);
    }
    final CloudSolrClient cloudSolrClient = builder.build();

    try (ClientCleanup clientCleanup = new ClientCleanup(cloudSolrClient)) {
      Set<String> nodes = cloudSolrClient.getClusterStateProvider().getLiveNodes();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Connected Nodes: [{}]", String.join(", ", nodes));
      }
      clientCleanup.transferOwnership();
      return cloudSolrClient;
    }
  }

  /**
   * Owns a client during initialization, until ownership passes to the caller.
   */
  private static final class ClientCleanup implements AutoCloseable {

    private final Closeable client;
    private boolean ownershipTransferred;

    private ClientCleanup(Closeable client) {
      this.client = client;
    }

    private void transferOwnership() {
      ownershipTransferred = true;
    }

    @Override
    public void close() {
      if (!ownershipTransferred) {
        try {
          client.close();
        } catch (IOException closeFailure) {
          throw new UncheckedIOException("Failed to close Solr client during initialization", closeFailure);
        }
      }
    }
  }

  /**
   * This utility method converts an address (host plus port) to a string that is accepted by {@link CloudSolrClient}.
   *
   * @param address The address to convert.
   * @return The compliant string.
   */
  static String toCloudSolrClientAddressString(InetSocketAddress address) {
    return address.getHostString() + ":" + address.getPort();
  }
}

