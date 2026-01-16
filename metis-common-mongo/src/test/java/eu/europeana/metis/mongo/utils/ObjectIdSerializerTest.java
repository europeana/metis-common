package eu.europeana.metis.mongo.utils;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ser.SerializationContextExt;

import java.io.StringWriter;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for class {@link ObjectIdSerializer}
 *
 * @author Jorge Ortiz
 * @since 31-01-2022
 */
class ObjectIdSerializerTest {

  private ObjectIdSerializer objectIdSerializer;

  @BeforeEach
  void setup() {
    this.objectIdSerializer = new ObjectIdSerializer(ObjectId.class);
  }

  @Test
  void serializeValue_expectSuccess() {
    final String expectedObjectId = "\"507f1f77bcf86cd799439011\"";
    final ObjectId objectId = new ObjectId("507f1f77bcf86cd799439011");
    final Writer jsonWriter = new StringWriter();
    final ObjectMapper mapper = new ObjectMapper();
    final JsonGenerator jsonGenerator = getJsonGenerator(mapper, jsonWriter);

    objectIdSerializer.serialize(objectId, jsonGenerator, getSerializerProvider());
    jsonGenerator.flush();

    assertEquals(expectedObjectId, jsonWriter.toString());
  }

  @Test
  void serializeNull_expectSuccess() {
    final String expectedObjectId = "null";
    final Writer jsonWriter = new StringWriter();
    final ObjectMapper mapper = new ObjectMapper();
    final JsonGenerator jsonGenerator = getJsonGenerator(mapper, jsonWriter);

    objectIdSerializer.serialize(null, jsonGenerator, getSerializerProvider());
    jsonGenerator.flush();

    assertEquals(expectedObjectId, jsonWriter.toString());
  }

  private static JsonGenerator getJsonGenerator(ObjectMapper mapper, Writer writer) {
    return JsonFactory.builder()
            .build()
            .createGenerator(mapper._serializationContext(), writer);
  }

  private static SerializationContextExt getSerializerProvider() {
    return new ObjectMapper()._serializationContext();
  }
}