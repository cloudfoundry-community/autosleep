package org.cloudfoundry.autosleep.servicebroker.configuration;

import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;


@Configuration
public class CatalogConfiguration {

    private static final String OPTIN_GUID="78C0A1DB-ACC9-4B6D-AF22-A1EF63C2CE06";
    private static final String OPTOUT_GUID="FE16E9E3-0D61-4D65-8A2F-2AFDD093E674";

    @Bean(autowire = Autowire.BY_TYPE)
    public Catalog catalog() {
        return new Catalog(Arrays.asList(
                new ServiceDefinition(
                        "autosleep",
                        "autosleep",
                        "Service that put your application to sleep when inactive",
                        true,
                        false,
                        Arrays.asList(
                                new Plan(OPTIN_GUID,
                                        "opt-in",
                                        "This is a default autosleep plan.  Binded apps will be stopped when idle."),
                                new Plan(OPTOUT_GUID,
                                        "opt-out",
                                        "All apps will be stopped when idle, EXCEPTED binded apps.")),
                        Arrays.asList("autosleep", "document"),
                        getServiceDefinitionMetadata(),
                        null,
                        null)));
    }

    /* Used by Pivotal CF console */

    private Map<String, Object> getServiceDefinitionMetadata() {
        Map<String, Object> sdMetadata = new HashMap<>();
        sdMetadata.put("displayName", "Autosleep");
        sdMetadata.put("imageUrl", "https://en.wikipedia"
                + ".org/wiki/Sleep#/media/File:WLA_metmuseum_Bronze_statue_of_Eros_sleeping_7.jpg");
        sdMetadata.put("longDescription", "Autosleep Service");
        sdMetadata.put("providerDisplayName", "Orange");
        sdMetadata.put("documentationUrl", "https://github.com/Orange-OpenSource/autosleep");
        sdMetadata.put("supportUrl", "https://github.com/Orange-OpenSource/autosleep");
        return sdMetadata;
    }


}
