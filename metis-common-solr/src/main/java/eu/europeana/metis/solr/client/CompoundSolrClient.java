package eu.europeana.metis.solr.client;

import java.io.Closeable;
import java.io.IOException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

/**
 * A Solr client that owns its HTTP and cloud clients and an optional underlying transport client.
 */
public class CompoundSolrClient implements Closeable {

    private final SolrClient httpSolrClient;
    private final CloudSolrClient cloudSolrClient;
    private final SolrClient transportClient;

    /**
     * Constructor with solr client parameters
     *
     * @param solrClient  the solr client
     * @param cloudSolrClient the cloud solr client
     */
    public CompoundSolrClient(SolrClient solrClient, CloudSolrClient cloudSolrClient) {
        this(solrClient, cloudSolrClient, null);
    }

    /**
     * Creates a client that also owns a separately managed transport client.
     *
     * @param solrClient the HTTP client, which may wrap the transport client
     * @param cloudSolrClient the optional cloud client
     * @param transportClient the optional underlying client, distinct from the other clients,
     *                        to close after its consumers
     */
    public CompoundSolrClient(SolrClient solrClient, CloudSolrClient cloudSolrClient,
                              SolrClient transportClient) {
        this.httpSolrClient = solrClient;
        this.cloudSolrClient = cloudSolrClient;
        this.transportClient = transportClient;
    }

    public SolrClient getSolrClient() {
        return cloudSolrClient == null ? httpSolrClient : cloudSolrClient;
    }

    @Override
    public void close() throws IOException {
        try (transportClient; httpSolrClient) {
            // Close the cloud client first, then HTTP and transport in reverse resource order.
            if (cloudSolrClient != null) {
                cloudSolrClient.close();
            }
        }
    }
}
