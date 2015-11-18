package org.cloudfoundry.autosleep.util.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.regex.Pattern;

public class PatternSerializer extends JsonSerializer<Pattern> {

    @Override
    public void serialize(Pattern pattern, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeString(pattern.pattern());
    }
}
