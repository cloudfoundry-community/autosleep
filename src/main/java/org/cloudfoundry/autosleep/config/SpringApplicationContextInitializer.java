package org.cloudfoundry.autosleep.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.cloud.service.common.RedisServiceInfo;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
public class SpringApplicationContextInitializer implements
        ApplicationContextInitializer<GenericApplicationContext> {


    private static final Map<Class<? extends ServiceInfo>, String> serviceTypeToProfileName =
            new HashMap<>();


    public static final String IN_MEMORY_PROFILE = "in-memory";

    private static final List<String> validLocalProfiles = Collections.singletonList("redis");

    static {
        serviceTypeToProfileName.put(RedisServiceInfo.class, "redis");
    }

    @Override
    public void initialize(GenericApplicationContext applicationContext) {
        log.debug("----------------------- initialize --------------------");
        Cloud cloud = getCloud();

        ConfigurableEnvironment appEnvironment = applicationContext.getEnvironment();

        String[] persistenceProfiles = getCloudProfile(cloud);
        if (persistenceProfiles == null) {
            persistenceProfiles = getActiveProfile(appEnvironment);
        }
        if (persistenceProfiles == null) {
            persistenceProfiles = new String[]{IN_MEMORY_PROFILE};
        }

        for (String persistenceProfile : persistenceProfiles) {
            appEnvironment.addActiveProfile(persistenceProfile);
        }
    }

    public String[] getCloudProfile(Cloud cloud) {
        if (cloud == null) {
            return null;
        }

        List<String> profiles = new ArrayList<String>();

        List<ServiceInfo> serviceInfos = cloud.getServiceInfos();

        log.info("Found serviceInfos: " + StringUtils.collectionToCommaDelimitedString(serviceInfos));

        for (ServiceInfo serviceInfo : serviceInfos) {
            if (serviceTypeToProfileName.containsKey(serviceInfo.getClass())) {
                profiles.add(serviceTypeToProfileName.get(serviceInfo.getClass()));
            }
        }

        if (profiles.size() > 1) {
            throw new IllegalStateException(
                    "Only one service of the following types may be bound to this application: "
                            + serviceTypeToProfileName.values().toString() + ". "
                            + "These services are bound to the application: ["
                            + StringUtils.collectionToCommaDelimitedString(profiles) + "]");
        }

        if (profiles.size() > 0) {
            return createProfileNames(profiles.get(0), "cloud");
        }

        return null;
    }

    private Cloud getCloud() {
        try {
            CloudFactory cloudFactory = new CloudFactory();
            return cloudFactory.getCloud();
        } catch (CloudException ce) {
            return null;
        }
    }

    private String[] getActiveProfile(ConfigurableEnvironment appEnvironment) {
        List<String> serviceProfiles = new ArrayList<String>();

        for (String profile : appEnvironment.getActiveProfiles()) {
            if (validLocalProfiles.contains(profile)) {
                serviceProfiles.add(profile);
            }
        }

        if (serviceProfiles.size() > 1) {
            throw new IllegalStateException("Only one active Spring profile may be set among the following: "
                    + validLocalProfiles.toString() + ". "
                    + "These profiles are active: ["
                    + StringUtils.collectionToCommaDelimitedString(serviceProfiles) + "]");
        }

        if (serviceProfiles.size() > 0) {
            return createProfileNames(serviceProfiles.get(0), "local");
        }

        return null;
    }

    private String[] createProfileNames(String baseName, String suffix) {
        String[] profileNames = {baseName, baseName + "-" + suffix};
        log.info("Setting profile names: " + StringUtils.arrayToCommaDelimitedString(profileNames));
        return profileNames;
    }
}
