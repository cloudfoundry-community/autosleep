package org.cloudfoundry.autosleep.servicebroker.configuration;

import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by BUCE8373 on 13/10/2015.
 */
@Configuration
@ComponentScan(basePackages = {"org.cloudfoundry.community.servicebroker", "org.cloudfoundry.autosleep.servicebroker"})
public class BrokerConfiguration {

    @Bean
    public BrokerApiVersion brokerApiVersion() {
        return new BrokerApiVersion("2.6");
    }

}
