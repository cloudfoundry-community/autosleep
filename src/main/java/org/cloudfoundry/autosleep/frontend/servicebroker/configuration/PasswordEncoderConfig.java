package org.cloudfoundry.autosleep.frontend.servicebroker.configuration;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

@Configuration
@Slf4j
public class PasswordEncoderConfig {
    @Autowired
    private Environment environment;

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        String secret = environment.getProperty(Config.EnvKey.CF_ENCODING_SECRET);
        if (secret == null) {
            log.debug("no secret used");
            return new StandardPasswordEncoder();
        } else {
            return new StandardPasswordEncoder(secret);
        }
    }
}
