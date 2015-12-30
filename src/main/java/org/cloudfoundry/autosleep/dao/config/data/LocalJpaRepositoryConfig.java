package org.cloudfoundry.autosleep.dao.config.data;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.dialect.H2Dialect;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@Profile("default")
@EnableJpaRepositories("org.cloudfoundry.autosleep.dao.repositories.jpa")
public class LocalJpaRepositoryConfig extends AbstractJpaRepositoryConfig {

    @PostConstruct
    public void logProfile() {
        log.warn("<<<<<<<<<<<  Warning: loading IN MEMORY persistance profile >>>>>>>>>>>>>>>>>>");
    }

    @Override
    protected String getHibernateDialect() {
        return H2Dialect.class.getName();
    }
}
