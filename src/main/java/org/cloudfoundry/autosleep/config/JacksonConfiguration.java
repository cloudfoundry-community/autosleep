package org.cloudfoundry.autosleep.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class JacksonConfiguration {

    /** For Jackson to be able to serialize/deserialize java.util.time format. */
    @Bean
    public JavaTimeModule javaTimeModule() {
        log.debug("------initializing Java time Module---------");
        return new JavaTimeModule();
    }
}