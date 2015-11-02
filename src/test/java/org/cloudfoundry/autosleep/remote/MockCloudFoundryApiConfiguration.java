package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@Slf4j
public class MockCloudFoundryApiConfiguration {


    @Bean
    @Primary
    public ClientConfiguration buildConfiguration() throws MalformedURLException {
        log.debug("buildConfiguration");
        return new ClientConfiguration(new URL("http://somewhere.org"), false, "clientId", "clientSecret",
                "username", "password");
    }

    @Bean
    @Primary
    public CloudFoundryApiService buildCloudfoundryApi() {
        log.debug("buildCloudfoundryApi");
        return Mockito.mock(CloudFoundryApiService.class);
    }
}
