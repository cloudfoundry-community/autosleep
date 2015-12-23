package org.cloudfoundry.autosleep.dao.config;

import org.cloudfoundry.autosleep.dao.config.data.InMemoryConfig;
import org.cloudfoundry.autosleep.dao.repositories.SpaceEnrollerConfigRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {SpaceEnrollerConfigRepository.class, InMemoryConfig.class})
public class RepositoryConfig {
}

