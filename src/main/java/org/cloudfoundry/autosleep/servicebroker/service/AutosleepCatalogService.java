package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.Getter;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.service.CatalogService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@Service
public class AutosleepCatalogService implements CatalogService {

    private static final String OPTIN_GUID = "78C0A1DB-ACC9-4B6D-AF22-A1EF63C2CE06";
    private static final String OPTOUT_GUID = "FE16E9E3-0D61-4D65-8A2F-2AFDD093E674";

    @Getter
    private Catalog catalog;

    private ServiceDefinition serviceDefinition;

    public AutosleepCatalogService() {
        serviceDefinition = new ServiceDefinition(
                "autosleep",
                "autosleep",
                "Service that put your application to sleep when inactive",
                true,
                false,
                Collections.singletonList(
                        new Plan(OPTIN_GUID,
                                "default",
                                "Autosleep default plan",
                                null,
                                true)),
                Arrays.asList("autosleep", "document"),
                getServiceDefinitionMetadata(),
                null,
                null);
        catalog = new Catalog(Collections.singletonList(serviceDefinition));
    }

    @Override
    public ServiceDefinition getServiceDefinition(String serviceId) {
        if (serviceDefinition.getId().equals(serviceId)) {
            return serviceDefinition;
        } else {
            return null;
        }
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
