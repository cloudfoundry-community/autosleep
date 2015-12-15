package org.cloudfoundry.autosleep.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;

@Configuration
@Slf4j
public class DeployedApplicationConfig {

    @Autowired
    private Environment environment;



    @Bean
    public Deployment loadCurrentDeployment() throws IOException {
        String deployment = environment.getProperty(Config.EnvKey.APPLICATION_DESCRIPTION_ENVIRONMENT_KEY);
        if (deployment == null) {
            return null;
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(deployment, Deployment.class);
        }
    }

}
