package org.cloudfoundry.autosleep.business;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.business.scheduling.AbstractPeriodicTask;
import org.cloudfoundry.autosleep.business.scheduling.Clock;
import org.cloudfoundry.autosleep.config.DeployedApplicationConfig;
import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.business.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.business.remote.ApplicationIdentity;
import org.cloudfoundry.autosleep.business.remote.CloudFoundryException;
import org.cloudfoundry.autosleep.business.remote.EntityNotFoundException;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class SpaceEnroller extends AbstractPeriodicTask {


    private final String serviceInstanceId;

    private final CloudFoundryApiService cloudFoundryApi;

    private final ServiceRepository serviceRepository;

    private final ApplicationRepository applicationRepository;

    private DeployedApplicationConfig.Deployment deployment;

    @Builder
    SpaceEnroller(Clock clock, Duration period, String serviceInstanceId,
                  CloudFoundryApiService cloudFoundryApi, ServiceRepository serviceRepository,
                  ApplicationRepository applicationRepository,
                  DeployedApplicationConfig.Deployment deployment) {
        super(clock, period);
        this.serviceInstanceId = serviceInstanceId;
        this.cloudFoundryApi = cloudFoundryApi;
        this.serviceRepository = serviceRepository;
        this.applicationRepository = applicationRepository;
        this.deployment = deployment;
    }

    @Override
    public void run() {
        SpaceEnrollerConfig serviceInstance = serviceRepository.findOne(serviceInstanceId);
        if (serviceInstance != null) {
            try {
                Set<String> watchedOrIgnoredApplications = new HashSet<>();
                applicationRepository.findAll()
                        .forEach(applicationInfo -> {
                            if (!applicationInfo.getEnrollmentState().isCandidate(serviceInstanceId)) {
                                watchedOrIgnoredApplications.add(applicationInfo.getUuid());
                            }
                        });
                log.debug("{} local applications (already watched, or to be ignored)",
                        watchedOrIgnoredApplications.size());
                List<ApplicationIdentity> applicationIdentities = cloudFoundryApi
                        .listApplications(UUID.fromString(serviceInstance.getSpaceId()),
                                serviceInstance.getExcludeFromAutoEnrollment());
                List<ApplicationIdentity> newApplications = applicationIdentities.stream()
                        .filter(application ->
                                deployment == null || !deployment.getApplicationId().equals(application.getGuid()))
                        .filter(application -> !(watchedOrIgnoredApplications.contains(application.getGuid())))
                        .collect(Collectors.toList());
                if (!newApplications.isEmpty()) {
                    log.debug("{} - new applications", newApplications.size());
                    cloudFoundryApi.bindServiceInstance(newApplications, serviceInstance.getServiceInstanceId());
                } else {
                    log.debug("all applications are binded");
                }
            } catch (EntityNotFoundException n) {
                log.error("service not found. should not appear cause should not be in repository anymore", n);
            } catch (CloudFoundryException c) {
                log.error("remote error", c);
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
