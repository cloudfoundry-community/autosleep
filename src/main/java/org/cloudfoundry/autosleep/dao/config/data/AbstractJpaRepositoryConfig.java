/*
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.autosleep.dao.config.data;

import org.cloudfoundry.autosleep.dao.model.SpaceEnrollerConfig;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractJpaRepositoryConfig {

    protected LocalContainerEntityManagerFactoryBean createEntityManagerFactoryBean(DataSource dataSource,
                                                                                    String dialectClassName) {
        Map<String, String> properties = new HashMap<>();
        properties.put(org.hibernate.cfg.Environment.HBM2DDL_AUTO, "update");
        properties.put(org.hibernate.cfg.Environment.DIALECT, dialectClassName);
        properties.put(org.hibernate.cfg.Environment.SHOW_SQL, "false");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(SpaceEnrollerConfig.class.getPackage().getName());
        em.setPersistenceProvider(new HibernatePersistenceProvider());
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        return createEntityManagerFactoryBean(dataSource, getHibernateDialect());
    }

    protected abstract String getHibernateDialect();

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

}
