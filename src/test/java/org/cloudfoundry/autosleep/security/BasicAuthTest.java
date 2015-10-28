package org.cloudfoundry.autosleep.security;

import org.cloudfoundry.autosleep.AbstractRestTest;
import org.cloudfoundry.community.servicebroker.controller.CatalogController;
import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BasicAuthTest extends AbstractRestTest {


    @Value("${security.user.name}")
    private String username;

    @Value("${security.user.password}")
    private String password;

    @Autowired
    private BrokerApiVersion apiVersion;

    @Test
    public void userCallWithoutCredentials() {
        assertThat(prepare(CatalogController.BASE_PATH, HttpMethod.GET, null, Catalog.class).call().getStatusCode(),
                is(equalTo(HttpStatus.UNAUTHORIZED)));
    }

    @Test
    public void userCallWithWrongCredentials() {
        assertThat(prepare(CatalogController.BASE_PATH, HttpMethod.GET, null, Catalog.class)
                        .withBasicAuthentication(username, password + "123").call().getStatusCode(),
                is(equalTo(HttpStatus.UNAUTHORIZED)));
    }

    @Test
    public void userCallWithGoodCredentials() {
        assertThat(
                prepare(CatalogController.BASE_PATH, HttpMethod.GET, null, Catalog.class)
                        .withBasicAuthentication(username, password)
                        .withHeader(BrokerApiVersion.DEFAULT_API_VERSION_HEADER,
                                apiVersion.getApiVersion()).call().getStatusCode(),
                is(equalTo(HttpStatus.OK)));
    }
}
