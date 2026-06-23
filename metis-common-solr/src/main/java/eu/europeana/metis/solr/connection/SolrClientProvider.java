package eu.europeana.metis.solr.connection;

import eu.europeana.metis.solr.client.CompoundSolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
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
     * Creates a Solr client from the properties. This method can be called multiple times and will
     * return a different client each time.
     *
     * @return A Solr client.
     * @throws E In case there is a problem with the supplied properties.
     */
    public CompoundSolrClient createSolrClient() throws E {
        final LBJettySolrClient httpSolrClient = setUpHttpSolrConnection();
        final CloudSolrClient cloudSolrClient;
        if (settings.hasZookeeperConnection()) {
            cloudSolrClient = setUpCloudSolrConnection();
        } else {
            cloudSolrClient = null;
        }
        return new CompoundSolrClient(httpSolrClient, cloudSolrClient);
    }

    private LBJettySolrClient setUpHttpSolrConnection() throws E {
        final Endpoint[] solrHosts =
                settings.getSolrHosts().stream().map(host -> new Endpoint(host.toString())).toArray(Endpoint[]::new);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Connecting to Solr hosts: [{}]",
                    String.join(", ", Arrays.stream(solrHosts).map(Endpoint::toString).toArray(String[]::new)));
        }

        HttpJettySolrClient baseClient = new HttpJettySolrClient.Builder()
                .withConnectionTimeout(settings.getSolrClientConnectionTimeoutInSecs(), TimeUnit.SECONDS)
                .withIdleTimeout(settings.getSolrClientIdleConnectionTimeoutInSecs(), TimeUnit.SECONDS)
                .useHttp1_1(settings.getSolrUseHttp1())
                .build();
        return new LBJettySolrClient.Builder(baseClient, solrHosts).build();
    }

    private CloudSolrClient setUpCloudSolrConnection() throws E {

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
        final CloudSolrClient cloudSolrClient = builder.build();

        Set<String> nodes = cloudSolrClient.getClusterStateProvider().getLiveNodes();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Connected Nodes: [{}]", String.join(", ", nodes));
        }

        // Done
        return cloudSolrClient;
    }

    /**
     * This utility method converts an address (host plus port) to a string that is accepted by {@link
     * CloudSolrClient}.
     *
     * @param address The address to convert.
     * @return The compliant string.
     */
    static String toCloudSolrClientAddressString(InetSocketAddress address) {
        return address.getHostString() + ":" + address.getPort();
    }
}

