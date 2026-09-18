package eu.europeana.metis.solr.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient;
import org.apache.solr.client.solrj.jetty.LBJettySolrClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    void closeConsumersBeforeTransport() throws IOException {
        final CloudSolrClient cloud = mock(CloudSolrClient.class);
        final LBJettySolrClient loadBalancer = mock(LBJettySolrClient.class);
        final HttpJettySolrClient transport = mock(HttpJettySolrClient.class);

        new CompoundSolrClient(loadBalancer, cloud, transport).close();

        var order = inOrder(cloud, loadBalancer, transport);
        order.verify(cloud).close();
        order.verify(loadBalancer).close();
        order.verify(transport).close();
        order.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void closeAllClientsAndSuppressLaterFailures(boolean uncheckedFailure) {
        final CloudSolrClient cloud = mock(CloudSolrClient.class);
        final LBJettySolrClient loadBalancer = mock(LBJettySolrClient.class);
        final HttpJettySolrClient transport = mock(HttpJettySolrClient.class);
        final Exception first = uncheckedFailure
                ? new IllegalStateException("cloud close failed") : new IOException("cloud close failed");
        final RuntimeException second = new IllegalStateException("load balancer close failed");
        final RuntimeException third = new IllegalStateException("transport close failed");
        doThrow(first).when(cloud).close();
        doThrow(second).when(loadBalancer).close();
        doThrow(third).when(transport).close();

        Exception thrown = assertThrows(Exception.class,
                () -> new CompoundSolrClient(loadBalancer, cloud, transport).close());

        assertSame(first, thrown);
        assertArrayEquals(new Throwable[]{second, third}, thrown.getSuppressed());
        var order = inOrder(cloud, loadBalancer, transport);
        order.verify(cloud).close();
        order.verify(loadBalancer).close();
        order.verify(transport).close();
    }
}
