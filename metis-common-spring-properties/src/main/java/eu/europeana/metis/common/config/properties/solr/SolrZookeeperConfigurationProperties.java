package eu.europeana.metis.common.config.properties.solr;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Class using {@link ConfigurationProperties} loading.
 */
@ConfigurationProperties(prefix = "solr")
public class SolrZookeeperConfigurationProperties {

  private String[] hosts;
  private boolean useHttp1;

  @NestedConfigurationProperty
  //Keep the name as is(zookeeper) for the spring mapping.
  private ZookeeperConfigurationProperties zookeeper;

  public String[] getHosts() {
    return hosts == null ? null : hosts.clone();
  }

  public void setHosts(String[] hosts) {
    this.hosts = hosts == null ? null : hosts.clone();
  }

  public boolean isUseHttp1() {
    return useHttp1;
  }

  public void setUseHttp1(boolean useHttp1) {
    this.useHttp1 = useHttp1;
  }

  public ZookeeperConfigurationProperties getZookeeper() {
    return zookeeper;
  }

  public void setZookeeper(ZookeeperConfigurationProperties zookeeper) {
    this.zookeeper = zookeeper;
  }
}
