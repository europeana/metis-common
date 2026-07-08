package eu.europeana.metis.mongo.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.query.FindOptions;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.filters.Filters;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.edm.exceptions.MongoDBException;
import eu.europeana.corelib.edm.exceptions.MongoRuntimeException;
import eu.europeana.corelib.edm.model.metainfo.AudioMetaInfoImpl;
import eu.europeana.corelib.edm.model.metainfo.ImageMetaInfoImpl;
import eu.europeana.corelib.edm.model.metainfo.TextMetaInfoImpl;
import eu.europeana.corelib.edm.model.metainfo.ThreeDMetaInfoImpl;
import eu.europeana.corelib.edm.model.metainfo.VideoMetaInfoImpl;
import eu.europeana.corelib.edm.model.metainfo.WebResourceMetaInfoImpl;
import eu.europeana.corelib.record.api.WebMetaInfo;
import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.corelib.solr.derived.AttributionSnippet;
import eu.europeana.corelib.solr.entity.AddressImpl;
import eu.europeana.corelib.solr.entity.AgentImpl;
import eu.europeana.corelib.solr.entity.AggregationImpl;
import eu.europeana.corelib.solr.entity.BasicProxyImpl;
import eu.europeana.corelib.solr.entity.ChangeLogImpl;
import eu.europeana.corelib.solr.entity.ConceptImpl;
import eu.europeana.corelib.solr.entity.ConceptSchemeImpl;
import eu.europeana.corelib.solr.entity.DatasetImpl;
import eu.europeana.corelib.solr.entity.EuropeanaAggregationImpl;
import eu.europeana.corelib.solr.entity.EventImpl;
import eu.europeana.corelib.solr.entity.LicenseImpl;
import eu.europeana.corelib.solr.entity.OrganizationImpl;
import eu.europeana.corelib.solr.entity.PersistentIdentifierImpl;
import eu.europeana.corelib.solr.entity.PhysicalThingImpl;
import eu.europeana.corelib.solr.entity.PlaceImpl;
import eu.europeana.corelib.solr.entity.ProvidedCHOImpl;
import eu.europeana.corelib.solr.entity.ProxyImpl;
import eu.europeana.corelib.solr.entity.QualityAnnotationImpl;
import eu.europeana.corelib.solr.entity.ServiceImpl;
import eu.europeana.corelib.solr.entity.TimespanImpl;
import eu.europeana.corelib.solr.entity.WebResourceImpl;
import eu.europeana.corelib.web.exception.EuropeanaException;
import eu.europeana.corelib.web.exception.ProblemType;

import java.util.Optional;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connection for accessing Europeana records.
 */
public class RecordDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecordDao.class);
  private static final String ABOUT  = "about";
  private final Datastore datastore;

  /**
   * Constructor to initialize the mongo mappings/collections and the {@link Datastore} connection.
   * This constructor is meant to be used when the database is already available.
   *
   * @param mongoClient the mongo client connection
   * @param databaseName the database name of the record redirect database
   */
  public RecordDao(MongoClient mongoClient, String databaseName) {
    this(mongoClient, databaseName, false);
  }

  /**
   * Constructor to initialize the mongo mappings/collections and the {@link Datastore} connection.
   * This constructor is meant to be used mostly for when the creation of the database is required.
   *
   * @param mongoClient the mongo client connection
   * @param databaseName the database name of the record redirect database
   * @param createIndexes flag that initiates the database/indices
   */
  public RecordDao(MongoClient mongoClient, String databaseName, boolean createIndexes) {
    this.datastore = createDatastore(mongoClient, databaseName);
    if (createIndexes) {
      LOGGER.info("Initializing database indices");
      datastore.ensureIndexes();
    }
  }

  private Datastore createDatastore(MongoClient mongoClient, String databaseName) {
    final Datastore morphiaDatastore = Morphia.createDatastore(mongoClient, databaseName);
    final Mapper mapper = morphiaDatastore.getMapper();
    mapper.getEntityModel(FullBeanImpl.class);
    mapper.getEntityModel(ProvidedCHOImpl.class);
    mapper.getEntityModel(AgentImpl.class);
    mapper.getEntityModel(AddressImpl.class);
    mapper.getEntityModel(AggregationImpl.class);
    mapper.getEntityModel(OrganizationImpl.class);
    mapper.getEntityModel(ConceptImpl.class);
    mapper.getEntityModel(ProxyImpl.class);
    mapper.getEntityModel(PlaceImpl.class);
    mapper.getEntityModel(TimespanImpl.class);
    mapper.getEntityModel(WebResourceImpl.class);
    mapper.getEntityModel(EuropeanaAggregationImpl.class);
    mapper.getEntityModel(ChangeLogImpl.class);
    mapper.getEntityModel(EventImpl.class);
    mapper.getEntityModel(PhysicalThingImpl.class);
    mapper.getEntityModel(ConceptSchemeImpl.class);
    mapper.getEntityModel(BasicProxyImpl.class);
    mapper.getEntityModel(WebResourceMetaInfoImpl.class);
    mapper.getEntityModel(LicenseImpl.class);
    mapper.getEntityModel(ServiceImpl.class);
    mapper.getEntityModel(QualityAnnotationImpl.class);
    mapper.getEntityModel(PersistentIdentifierImpl.class);
    mapper.getEntityModel(AttributionSnippet.class);
    mapper.getEntityModel(DatasetImpl.class);
    mapper.getEntityModel(ImageMetaInfoImpl.class);
    mapper.getEntityModel(AudioMetaInfoImpl.class);
    mapper.getEntityModel(TextMetaInfoImpl.class);
    mapper.getEntityModel(VideoMetaInfoImpl.class);
    mapper.getEntityModel(ThreeDMetaInfoImpl.class);
    LOGGER.info("Datastore initialized");

    return morphiaDatastore;
  }

  public Datastore getDatastore() {
    return this.datastore;
  }


  /**
   * Retrieves a record from the datastore using the specified identifier.
   *
   * @param id the unique identifier of the record, corresponding to the {@code about} field
   * @return an {@link Optional} containing the {@link FullBean} if found, otherwise an empty {@link Optional}
   * @throws EuropeanaException if an error occurs during the retrieval process
   */
  public Optional<FullBean> getRecord(String id) throws EuropeanaException {
    return getRecords(Filters.eq(ABOUT, id), new FindOptions()).findFirst();
  }

  /**
   * Checks if a record exists in the datastore based on the provided identifier.
   *
   * @param id the unique identifier of the record, corresponding to the {@code about} field
   * @return {@code true} if the record exists, {@code false} otherwise
   * @throws EuropeanaException if an error occurs during the lookup process
   */
  public boolean hasRecord(String id) throws EuropeanaException {
    try {
      return (getDatastore().find(FullBeanImpl.class)
              .filter(Filters.eq(ABOUT, id))
              .count() > 0);
    } catch (RuntimeException re) {
      throw processException(re);
    }
  }

  /**
   * Retrieves a stream of {@link FullBean} objects based on a collection of unique identifiers.
   *
   * @param ids a collection of unique identifiers corresponding to the {@code about} field of the records
   * @return a {@link Stream} of {@link FullBean} objects matching the provided identifiers
   * @throws EuropeanaException if an error occurs during the retrieval process
   */
  public Stream<FullBean> getRecords(Collection<String> ids) throws EuropeanaException {
    FindOptions opts = new FindOptions().batchSize(ids.size());
    return getRecords(Filters.in(ABOUT, ids), opts);
  }

  /**
   * Retrieves a stream of {@link FullBean} objects based on the provided filter and find options.
   *
   * @param filter the filter criteria to apply when fetching the records
   * @param opts   the find options specifying how the records should be retrieved, such as sorting and pagination
   * @return a {@link Stream} of {@link FullBean} objects that match the provided filter and find options
   * @throws EuropeanaException if an error occurs during the retrieval process
   */
  @SuppressWarnings("java:S5738")
  public Stream<FullBean> getRecords(Filter filter, FindOptions opts) throws EuropeanaException {
    try {
      return getDatastore().find(FullBeanImpl.class).filter(filter)
              .stream(opts).map(this::injectWebMeta);
    } catch (RuntimeException re) {
      throw processException(re);
    }
  }

  /**
   * Injects web metadata information into the provided {@link FullBean} instance.
   * Note: the {@link WebMetaInfo#injectWebMetaInfoBatch(FullBean, RecordDao, String)}
   *       will ultimately call {@link #retrieveWebMetaInfos} method
   *
   * @param bean the {@link FullBean} instance to inject metadata information into
   * @return the {@link FullBean} instance with the associated web metadata information
   */
  protected FullBean injectWebMeta(FullBean bean) {
    WebMetaInfo.injectWebMetaInfoBatch(bean, this, null);
    return bean;
  }

  /**
   * Get a full bean using an identifier matching it's {@code about} field.
   *
   * @param id the identifier of the fullbean
   * @return the matched full bean
   * @throws EuropeanaException if anything when wrong with the request
   */
  public FullBean getFullBean(String id) throws EuropeanaException {
    try {
      long start = 0;
      if (LOGGER.isDebugEnabled()) {
        start = System.currentTimeMillis();
      }
      FullBeanImpl result = datastore.find(FullBeanImpl.class).filter(Filters.eq(ABOUT, id))
            .first();
      LOGGER.debug("Mongo query find fullbean {} finished in {} ms", id,
          (System.currentTimeMillis() - start));
      return result;
    } catch (RuntimeException re) {
      throw processException(re);
    }
  }

  /**
   * Find Web resource metadata matches using a list of hash codes.
   *
   * @param hashCodes the hash codes
   * @return a map of the web resource metadata id and the metadata corresponding to that id
   */
  public Map<String, WebResourceMetaInfoImpl> retrieveWebMetaInfos(List<String> hashCodes) {
    Map<String, WebResourceMetaInfoImpl> metaInfos = new HashMap<>();

    final BasicDBObject basicObject = new BasicDBObject("$in", hashCodes);
    long start = 0;
    if (LOGGER.isDebugEnabled()) {
      start = System.currentTimeMillis();
    }
    List<WebResourceMetaInfoImpl> metaInfoList = getDatastore().find(WebResourceMetaInfoImpl.class)
        .disableValidation().filter(Filters.eq("_id", basicObject)).iterator().toList();
    LOGGER.debug("Mongo query find metainfo for {} webresources done in {} ms", hashCodes.size(),
        (System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    metaInfoList.forEach(cursor -> {
      String id = cursor.getId();
      metaInfos.put(id, cursor);
    });
    LOGGER.debug("Mongo cursor done in {} ms", (System.currentTimeMillis() - start));
    return metaInfos;

  }

  @Override
  public String toString() {
    return "{ datastore=" + datastore.getDatabase().getName() + " }";
  }

  /**
   * Get a document using a class type and an about value.
   *
   * @param clazz the class representing type
   * @param about the about value
   * @param <T> the type
   * @return the object found
   */
  public <T> T searchByAbout(Class<T> clazz, String about) {
    return datastore.find(clazz).filter(Filters.eq(ABOUT, about)).first();
  }

  /**
   * Processes a {@link RuntimeException} and maps it to a specific {@link EuropeanaException}.
   * Determines the exception type based on the cause and provides an appropriate error context.
   *
   * @param re the runtime exception to process
   * @return a {@link EuropeanaException} instance representing the processed exception
   */
  protected EuropeanaException processException(RuntimeException re) {
    if (re.getCause() != null && (re.getCause() instanceof MappingException
            || re.getCause() instanceof ClassCastException)) {
      return new MongoDBException(ProblemType.RECORD_RETRIEVAL_ERROR, re);
    } else {
      return new MongoRuntimeException(ProblemType.MONGO_UNREACHABLE, re);
    }
  }
}
