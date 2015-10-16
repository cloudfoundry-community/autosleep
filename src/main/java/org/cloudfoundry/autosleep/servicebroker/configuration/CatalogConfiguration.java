package org.cloudfoundry.autosleep.servicebroker.configuration;

import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Configuration
public class CatalogConfiguration {
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
                                new Plan("autosleep-plan",
                                        "Default autosleep Plan",
                                        "This is a default autosleep plan.  All services are created equally.",
                                        getPlanMetadata())),
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

    private Map<String, Object> getPlanMetadata() {
        Map<String, Object> planMetadata = new HashMap<>();
        planMetadata.put("costs", getCosts());
        planMetadata.put("bullets", getBullets());
        return planMetadata;
    }

    private List<Map<String, Object>> getCosts() {
        Map<String, Object> costsMap = new HashMap<>();

        Map<String, Object> amount = new HashMap<>();
        amount.put("eur", 0.0D);

        costsMap.put("amount", amount);
        costsMap.put("unit", "MONTHLY");

        return Collections.singletonList(costsMap);
    }

    private List<String> getBullets() {
        return Collections.singletonList("Shared autosleep service");
    }
}
