package eu.europeana.metis.solr.client;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.jetty.LBJettySolrClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit test for {@link CompoundSolrClient}
 *
 */
class CompoundSolrClientTest {

    private CompoundSolrClient compoundSolrClient;

    @Test
    void getSolrClient() {
        final LBJettySolrClient lbHttpSolrClient = mock(LBJettySolrClient.class);
        final CloudSolrClient cloudSolrClient = mock(CloudSolrClient.class);

        compoundSolrClient = new CompoundSolrClient(lbHttpSolrClient, cloudSolrClient);

        assertNotNull(compoundSolrClient.getSolrClient());
        assertEquals(cloudSolrClient, compoundSolrClient.getSolrClient());
    }

    @Test
    void getSolrClientCloudSolrClientIsNull() {
        final LBJettySolrClient lbHttpSolrClient = mock(LBJettySolrClient.class);

        compoundSolrClient = new CompoundSolrClient(lbHttpSolrClient, null);

        assertNotNull(compoundSolrClient.getSolrClient());
        assertEquals(lbHttpSolrClient, compoundSolrClient.getSolrClient());
    }

    @Test
    void close() throws IOException {
        final LBJettySolrClient lbHttpSolrClient = mock(LBJettySolrClient.class);
        final CloudSolrClient cloudSolrClient = mock(CloudSolrClient.class);
        compoundSolrClient = new CompoundSolrClient(lbHttpSolrClient, cloudSolrClient);

        compoundSolrClient.close();

        verify(lbHttpSolrClient).close();
        verify(cloudSolrClient).close();
    }
}
