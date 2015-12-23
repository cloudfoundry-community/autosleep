package org.cloudfoundry.autosleep.dao.config.data;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.cloudfoundry.autosleep.dao.repositories.ram.RamApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.ram.RamBindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ram.RamSpaceEnrollerConfigRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@Profile("default")
public class InMemoryConfig {

    @PostConstruct
    public void logProfile() {
        log.warn("<<<<<<<<<<<  Warning: loading IN MEMORY persistance profile >>>>>>>>>>>>>>>>>>");
    }

    @Bean
    public SpaceEnrollerConfigRepository ramServiceRepository() {
        return new RamSpaceEnrollerConfigRepository();
    }

    @Bean
    public BindingRepository ramBindingRepository() {
        return new RamBindingRepository();
    }

    @Bean
    public ApplicationRepository ramAppRepository() {
        return new RamApplicationRepository();
    }

}
