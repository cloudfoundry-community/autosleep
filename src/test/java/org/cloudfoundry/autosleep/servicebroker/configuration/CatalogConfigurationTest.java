package org.cloudfoundry.autosleep.servicebroker.configuration;

import org.cloudfoundry.autosleep.AbstractRestTest;
import org.cloudfoundry.community.servicebroker.controller.CatalogController;
import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;



public class CatalogConfigurationTest extends AbstractRestTest{


    @Autowired
    private BrokerApiVersion apiVersion;

    @Autowired
    private Catalog catalog;


    @Test
    public void userForgetsTheHeader() {
        assertThat(prepare(CatalogController.BASE_PATH, HttpMethod.GET, null, Catalog.class)
                .withBasicAuthentication(username, password).call().getStatusCode(), is(equalTo(HttpStatus.OK)));
    }

    @Test
    public void userAsksTheWrongVersion() {
        assertThat(prepare(CatalogController.BASE_PATH, HttpMethod.GET, null, Catalog.class)
                .withBasicAuthentication(username, password).withHeader(BrokerApiVersion.DEFAULT_API_VERSION_HEADER,
                        apiVersion.getApiVersion() + ".2").call().getStatusCode(),
                is(equalTo(HttpStatus.OK)));
    }

    @Test
    public void userAsksTheCorrectVersion() {
        ResponseEntity<Catalog> response = prepare(CatalogController.BASE_PATH, HttpMethod.GET, null,
                Catalog.class)
                .withBasicAuthentication(username, password).withHeader(BrokerApiVersion.DEFAULT_API_VERSION_HEADER,
                        apiVersion.getApiVersion()).call();
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
        assertThat(response.getBody().getServiceDefinitions().size(), is(equalTo(1)));
        assertThat(response.getBody().getServiceDefinitions().size(), is(equalTo(1)));
        assertThat(response.getBody().getServiceDefinitions().get(0).getId(), is(equalTo(catalog
                .getServiceDefinitions().get(0).getId())));
    }

}
