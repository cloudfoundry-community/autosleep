package org.cloudfoundry.autosleep.scheduling;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Deployment;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.remote.ApplicationIdentity;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class ApplicationBinder extends AbstractPeriodicTask {


    private final String serviceInstanceId;

    private final CloudFoundryApiService cloudFoundryApi;

    private final ServiceRepository serviceRepository;

    private final ApplicationRepository applicationRepository;

    private Deployment deployment;

    @Builder
    ApplicationBinder(Clock clock, Duration period, String serviceInstanceId,
                      CloudFoundryApiService cloudFoundryApi, ServiceRepository serviceRepository,
                      ApplicationRepository applicationRepository,
                      Deployment deployment) {
        super(clock, period);
        this.serviceInstanceId = serviceInstanceId;
        this.cloudFoundryApi = cloudFoundryApi;
        this.serviceRepository = serviceRepository;
        this.applicationRepository = applicationRepository;
        this.deployment = deployment;
    }

    @Override
    public void run() {
        AutosleepServiceInstance serviceInstance = serviceRepository.findOne(serviceInstanceId);
        if (serviceInstance != null) {
            Set<UUID> watchedApplications = new HashSet<>();
            applicationRepository.findAll()
                    .forEach(applicationInfo -> watchedApplications.add(applicationInfo.getUuid()));
            log.debug("{} local applications", watchedApplications.size());
            List<ApplicationIdentity> applicationIdentities = cloudFoundryApi
                    .listApplications(UUID.fromString(serviceInstance.getSpaceGuid()),
                            serviceInstance.getExcludeNames());
            if (applicationIdentities != null) {
                List<ApplicationIdentity> newApplications = applicationIdentities.stream()
                        .filter(application ->
                                deployment == null || !deployment.getApplicationId().equals(application.getGuid()))
                        .filter(application -> !(watchedApplications.contains(application.getGuid())))
                        .collect(Collectors.toList());
                if (!newApplications.isEmpty()) {
                    log.debug("{} - new applications", newApplications.size());
                    cloudFoundryApi.bindServiceInstance(newApplications, serviceInstance.getServiceInstanceId());
                } else {
                    log.debug("all applications are binded");
                }
            } else {
                log.debug("no remote application returned");
            }
            rescheduleWithDefaultPeriod();
        } else {
            log.debug("service has been removed. Cancelling task");
            stopTask();
        }
    }

    @Override
    protected String getTaskId() {
        return serviceInstanceId;
    }
}
