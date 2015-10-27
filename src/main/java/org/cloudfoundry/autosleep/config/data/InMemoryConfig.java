package org.cloudfoundry.autosleep.config.data;

import org.cloudfoundry.autosleep.repositories.ram.RamServiceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

public class InMemoryConfig {

    @Bean
    public RamServiceRepository ramRepository() {
        return new RamServiceRepository();
    }


}
