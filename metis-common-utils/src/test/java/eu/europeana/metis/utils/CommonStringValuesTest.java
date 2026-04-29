package eu.europeana.metis.utils;

import static eu.europeana.metis.utils.CommonStringValues.BATCH_OF_DATASETS_RETURNED;
import static eu.europeana.metis.utils.CommonStringValues.DATE_FORMAT;
import static eu.europeana.metis.utils.CommonStringValues.DATE_FORMAT_FOR_REQUEST_PARAM;
import static eu.europeana.metis.utils.CommonStringValues.DATE_FORMAT_Z;
import static eu.europeana.metis.utils.CommonStringValues.EUROPEANA_ID_CREATOR_INITIALIZATION_FAILED;
import static eu.europeana.metis.utils.CommonStringValues.NEXT_PAGE_CANNOT_BE_NEGATIVE;
import static eu.europeana.metis.utils.CommonStringValues.PAGE_COUNT_CANNOT_BE_ZERO_OR_NEGATIVE;
import static eu.europeana.metis.utils.CommonStringValues.PLUGIN_EXECUTION_NOT_ALLOWED;
import static eu.europeana.metis.utils.CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE;
import static eu.europeana.metis.utils.CommonStringValues.UNAUTHORIZED;
import static eu.europeana.metis.utils.CommonStringValues.WRONG_ACCESS_TOKEN;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CommonStringValuesTest {

  @Test
  void testFieldsAreUsed() {
    assertNotNull(WRONG_ACCESS_TOKEN);
    assertNotNull(BATCH_OF_DATASETS_RETURNED);
    assertNotNull(NEXT_PAGE_CANNOT_BE_NEGATIVE);
    assertNotNull(PAGE_COUNT_CANNOT_BE_ZERO_OR_NEGATIVE);
    assertNotNull(PLUGIN_EXECUTION_NOT_ALLOWED);
    assertNotNull(UNAUTHORIZED);
    assertNotNull(EUROPEANA_ID_CREATOR_INITIALIZATION_FAILED);
    assertNotNull(DATE_FORMAT);
    assertNotNull(DATE_FORMAT_Z);
    assertNotNull(DATE_FORMAT_FOR_REQUEST_PARAM);
    assertNotNull(S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE);
  }
}

