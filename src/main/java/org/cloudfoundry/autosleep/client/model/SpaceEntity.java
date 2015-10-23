package org.cloudfoundry.autosleep.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpaceEntity extends AbstractEntity{

    @JsonProperty("apps_url")
    private String appsUrls;

    @JsonProperty("routes_url")
    private String routesUrl;

    @JsonProperty("domains_url")
    private String domainsUrl;

    @JsonProperty("service_instances_url")
    private String serviceInstancesUrl;

    @JsonProperty("app_events_url")
    private String appsEventsUrl;

    @JsonProperty("events_url")
    private String eventsUrl;
}
