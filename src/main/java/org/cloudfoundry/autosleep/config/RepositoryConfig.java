package org.cloudfoundry.autosleep.config;

import org.cloudfoundry.autosleep.config.data.InMemoryConfig;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {ServiceRepository.class, InMemoryConfig.class})
public class RepositoryConfig {
}

