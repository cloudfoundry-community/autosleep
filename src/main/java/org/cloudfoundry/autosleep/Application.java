package org.cloudfoundry.autosleep;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@Slf4j
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        log.debug("Application started");
    }

}
