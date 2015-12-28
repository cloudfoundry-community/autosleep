package org.cloudfoundry.autosleep.dao.config.data;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@Configuration
@Profile("default")
@EnableJpaRepositories("org.cloudfoundry.autosleep.dao.repositories.jpa")
public class H2DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setName("autosleep")
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}
