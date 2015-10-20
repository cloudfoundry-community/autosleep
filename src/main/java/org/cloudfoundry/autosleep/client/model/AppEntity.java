package org.cloudfoundry.autosleep.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppEntity extends AbstractEntity{

    private boolean production;

    private int memory;

    @JsonProperty("instances")
    private int nbInstances;

    private String state;


    @JsonProperty("package_updated_at")
    private String updatedAt;

    @JsonProperty("events_url")
    private String eventUrl;

    @JsonProperty("service_bindings_url")
    private String serviceBindingUrl;

    @JsonProperty("routes_url")
    private String routesUrl;

}
