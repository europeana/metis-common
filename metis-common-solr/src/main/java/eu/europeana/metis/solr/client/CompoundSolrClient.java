package eu.europeana.metis.solr.client;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.jetty.LBJettySolrClient;

import java.io.Closeable;
import java.io.IOException;

/**
 * This class represents a Solr client that can internally consist of two (closeable) clients.
 */
public class CompoundSolrClient implements Closeable {

    private final LBJettySolrClient httpSolrClient;
    private final CloudSolrClient cloudSolrClient;

    /**
     * Constructor with solr client parameters
     *
     * @param httpSolrClient  the load balanced solr client
     * @param cloudSolrClient the cloud solr client
     */
    public CompoundSolrClient(LBJettySolrClient httpSolrClient, CloudSolrClient cloudSolrClient) {
        this.httpSolrClient = httpSolrClient;
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
