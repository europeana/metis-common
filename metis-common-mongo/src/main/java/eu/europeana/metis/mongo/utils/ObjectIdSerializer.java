package eu.europeana.metis.mongo.utils;

import org.bson.types.ObjectId;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * A json serializer for {@link ObjectId} fields.
 */
public class ObjectIdSerializer extends StdSerializer<ObjectId> {

    /**
     * Default constructor.
     * Initializes the serializer with the {@link ObjectId} class as the target type.
     * Note: Required, do not remove.
     */
    public ObjectIdSerializer() {
        super(ObjectId.class);
    }

    protected ObjectIdSerializer(Class<?> t) {
        super(t);
    }

    @Override
    public void serialize(ObjectId value, JsonGenerator jgen, SerializationContext provider) throws JacksonException {
        if (value == null) {
            jgen.writeNull();
        } else {
            jgen.writeString(value.toString());
        }
    }
}
