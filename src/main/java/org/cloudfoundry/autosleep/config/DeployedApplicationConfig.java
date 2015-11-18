package org.cloudfoundry.autosleep.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
@Slf4j
public class DeployedApplicationConfig {
    static final String APPLICATION_DESCRIPTION_ENVIRONMENT_KEY = "VCAP_APPLICATION";

    @Autowired
    private Environment environment;


    @PostConstruct
    public void init() {
        log.debug("VCAP_APPLICATION={}", environment.getProperty(APPLICATION_DESCRIPTION_ENVIRONMENT_KEY));
    }

    @Bean
    public Deployment loadCurrentDeployment() throws IOException {
        String deployment = environment.getProperty(APPLICATION_DESCRIPTION_ENVIRONMENT_KEY);
        if (deployment == null) {
            return null;
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(deployment, Deployment.class);
        }
    }

    /*
    VCAP_APPLICATION={"limits":{"mem":1024,"disk":1024,"fds":16384},
    "application_id":"3048d795-f031-435f-85e8-71dce339e869",
    "application_version":"b546c9d4-8885-4d50-a855-490ddb5b5a1c",

    "application_name":"autosleep-app",
    "application_uris":["autosleep-app-ben.cf.ns.nd-paas.itn.ftgroup","autosleep-nonnational-artotype.cf.ns.nd-paas
    .itn.ftgroup","autosleep.cf.ns.nd-paas.itn.ftgroup"],
    "version":"b546c9d4-8885-4d50-a855-490ddb5b5a1c",
    "name":"autosleep-app",
    "space_name":"autosleep"
    ,"space_id":"2d745a4b-67e3-4398-986e-2adbcf8f7ec9",
    "uris":["autosleep-app-ben.cf.ns.nd-paas.itn.ftgroup",
    "autosleep-nonnational-artotype.cf.ns.nd-paas.itn.ftgroup",
    "autosleep.cf.ns.nd-paas.itn.ftgroup"]
    ,"users":null,
    "instance_id":"7984a682cab9447891674f862299c77f",
    "instance_index":0,
    "host":"0.0.0.0",
    "port":61302,
    "started_at":"2015-11-18 15:49:06 +0000",
    "started_at_timestamp":1447861746,
    "start":"2015-11-18 15:49:06 +0000",
    "state_timestamp":1447861746
    }
     */
}
