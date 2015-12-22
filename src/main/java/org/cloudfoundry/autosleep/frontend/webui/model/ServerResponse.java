package org.cloudfoundry.autosleep.frontend.webui.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cloudfoundry.autosleep.util.serializer.InstantDeserializer;
import org.cloudfoundry.autosleep.util.serializer.InstantSerializer;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponse<T> {
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant time;

    private T body;

}
