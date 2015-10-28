package org.cloudfoundry.autosleep.admin.controller;

import org.cloudfoundry.autosleep.AbstractRestTest;
import org.cloudfoundry.autosleep.remote.ClientConfiguration;
import org.cloudfoundry.autosleep.remote.ClientConfigurationBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigurationControllerTest extends AbstractRestTest {

    @Autowired
    private ClientConfigurationBuilder configurationBuilder;

    @Test
    public void testUpdateCredentials() {
        ClientConfiguration configuration = configurationBuilder.buildConfiguration();
        if (configuration != null) {
            ResponseEntity<String> result = prepare("/admin/configuration/",
                    HttpMethod.PUT, configuration, String.class)
                    .withBasicAuthentication(username, password).call();
            assertThat(result.getStatusCode(), is(equalTo(HttpStatus.OK)));
        }

    }

    @Test
    public void testCleanCredentials() {
        ResponseEntity<String> result = prepare("/admin/configuration/", HttpMethod.DELETE, null, String.class)
                .withBasicAuthentication(username, password).call();
        assertThat(result.getStatusCode(), is(equalTo(HttpStatus.NO_CONTENT)));
    }
}