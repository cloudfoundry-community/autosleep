package org.cloudfoundry.autosleep.dao.config;

import org.cloudfoundry.autosleep.dao.config.data.InMemoryConfig;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {ServiceRepository.class, InMemoryConfig.class})
public class RepositoryConfig {
}

