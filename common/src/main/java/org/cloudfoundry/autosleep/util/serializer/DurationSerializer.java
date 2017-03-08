package org.cloudfoundry.autosleep.util.serializer;

import java.io.IOException;
import java.time.Duration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class DurationSerializer extends JsonSerializer<Duration> {

    @Override
    public void serialize(Duration value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeString(value.toString());
    }

}
