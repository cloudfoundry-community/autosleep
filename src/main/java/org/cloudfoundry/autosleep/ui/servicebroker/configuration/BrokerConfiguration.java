package org.cloudfoundry.autosleep.ui.servicebroker.configuration;

import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerConfiguration {

    @Bean
    public BrokerApiVersion brokerApiVersion() {
        return new BrokerApiVersion();
    }

}
