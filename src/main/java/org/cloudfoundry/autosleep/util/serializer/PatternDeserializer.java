package org.cloudfoundry.autosleep.util.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternDeserializer extends JsonDeserializer<Pattern> {

    @Override
    public Pattern deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
        try {
            return Pattern.compile(jp.getValueAsString());
        } catch (PatternSyntaxException p) {
            throw new IOException("Invalid regexp", p);
        }
    }

}
