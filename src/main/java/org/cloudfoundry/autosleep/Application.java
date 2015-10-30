package org.cloudfoundry.autosleep;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.ContextInitializer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableWebMvc
@Slf4j
public class Application {
    
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
                .initializers(new ContextInitializer())
                .run(args);
        log.debug("Application started");
    }

}
