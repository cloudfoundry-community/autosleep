package org.cloudfoundry.autosleep.access.dao.config.data;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Configuration
@Slf4j
@Profile("postgresql-local")
public class PostgreSqlLocalDataSourceConfig {

    @PostConstruct
    public void logProfile() {
        log.warn("<<<<<<<<<<< loading POSTGRESQL persistence profile >>>>>>>>>>>>>>>>>>");
    }

    @Value("${postgresql.driver}")
    private String driver;

    @Value("${postgresql.password}")
    private String password;

    @Value("${postgresql.url}")
    private String url;

    @Value("${postgresql.username}")
    private String username;

    @Bean
    public DataSource dataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(url);
        dataSource.setDriverClassName(driver);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        
        return dataSource;
    }
}
