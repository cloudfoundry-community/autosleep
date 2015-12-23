package org.cloudfoundry.autosleep.dao.repositories.jpa;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp.BasicDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;


@Slf4j
public class JpaUtil {
    public static boolean isMySqlPresent() {
        final String propertiesLocation = "/application.properties";
        try (InputStream propertiesStream = JpaUtil.class.getResourceAsStream(propertiesLocation)) {
            Properties properties = new Properties();

            if (propertiesStream == null) {
                log.debug("{} cannot be found in classpath", propertiesLocation);
                return false;
            } else {
                properties.load(propertiesStream);
                final String driver = properties.getProperty("mysql.driver");
                final String url = properties.getProperty("mysql.url");
                final String username = properties.getProperty("mysql.username");
                final String password = properties.getProperty("mysql.password");
                BasicDataSource dataSource = new BasicDataSource();
                dataSource.setUrl(url);
                dataSource.setDriverClassName(driver);
                dataSource.setUsername(username);
                dataSource.setPassword(password);
                try ( Connection connection = dataSource.getConnection()) {
                    return connection != null;
                }
            }

        } catch (IOException e) {
            log.debug("Mysql: properties loading problem", e);
            return false;
        } catch (Throwable t) {
            log.debug("Mysql: not present", t);
            return false;
        }
    }
}
