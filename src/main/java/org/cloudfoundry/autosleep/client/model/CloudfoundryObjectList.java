package org.cloudfoundry.autosleep.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class CloudfoundryObjectList<T> {
    @JsonProperty("total_results")
    private int totalResults;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("prev_url")
    private String previousPageUrl;

    @JsonProperty("next_url")
    private String nextPageUrl;

    private Collection<CloudfoundryObject<T>> resources;
}
