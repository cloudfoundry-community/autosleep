package org.cloudfoundry.autosleep.config.data;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.repositories.ram.RamBindingRepository;
import org.cloudfoundry.autosleep.repositories.ram.RamServiceRepository;
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
        log.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  loading IN MEMORY profile   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }

    @Bean
    public RamServiceRepository ramServiceRepository() {
        log.debug("------------ loading IN MEMORY profile--------");
        return new RamServiceRepository();
    }

    @Bean
    public RamBindingRepository ramBindingRepository() {
        return new RamBindingRepository();
    }

}
