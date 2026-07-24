package eu.europeana.metis.solr.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link CompoundSolrClient}
 *
 */
class CompoundSolrClientTest {

    private CompoundSolrClient compoundSolrClient;

    @Test
    void getSolrClient() {
        final HttpJdkSolrClient httpJdkSolrClient = mock(HttpJdkSolrClient.class);
        final CloudSolrClient cloudSolrClient = mock(CloudSolrClient.class);

        compoundSolrClient = new CompoundSolrClient(httpJdkSolrClient, cloudSolrClient);

        assertNotNull(compoundSolrClient.getSolrClient());
        assertEquals(cloudSolrClient, compoundSolrClient.getSolrClient());
    }

    @Test
    void getSolrClientCloudSolrClientIsNull() {
        final HttpJdkSolrClient httpJdkSolrclient = mock(HttpJdkSolrClient.class);

        compoundSolrClient = new CompoundSolrClient(httpJdkSolrclient, null);

        assertNotNull(compoundSolrClient.getSolrClient());
        assertEquals(httpJdkSolrclient, compoundSolrClient.getSolrClient());
    }

    @Test
    void close() throws IOException {
        final HttpJdkSolrClient httpJdkSolrClient = mock(HttpJdkSolrClient.class);
        final CloudSolrClient cloudSolrClient = mock(CloudSolrClient.class);
        compoundSolrClient = new CompoundSolrClient(httpJdkSolrClient, cloudSolrClient);

        compoundSolrClient.close();

        verify(httpJdkSolrClient).close();
        verify(cloudSolrClient).close();
    }
}
