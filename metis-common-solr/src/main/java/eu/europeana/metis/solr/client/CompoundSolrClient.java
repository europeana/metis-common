package eu.europeana.metis.solr.client;

import java.io.Closeable;
import java.io.IOException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

/**
 * This class represents a Solr client that can internally consist of two (closeable) clients.
 */
public class CompoundSolrClient implements Closeable {

    private final SolrClient httpSolrClient;
    private final CloudSolrClient cloudSolrClient;

    /**
     * Constructor with solr client parameters
     *
     * @param solrClient  the solr client
     * @param cloudSolrClient the cloud solr client
     */
    public CompoundSolrClient(SolrClient solrClient, CloudSolrClient cloudSolrClient) {
        this.httpSolrClient = solrClient;
        this.cloudSolrClient = cloudSolrClient;
    }

    public SolrClient getSolrClient() {
        return cloudSolrClient == null ? httpSolrClient : cloudSolrClient;
    }

    @Override
    public void close() throws IOException {
        if (httpSolrClient != null) {
            httpSolrClient.close();
        }
        if (cloudSolrClient != null) {
            cloudSolrClient.close();
        }
    }
}
