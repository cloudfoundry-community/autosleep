package org.cloudfoundry.autosleep;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.controller.CatalogController;
import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by BUCE8373 on 13/10/2015.
 */
@Slf4j
public class CatalogStepDefinitions extends AbstractStep{
    @Value("${security.user.name}")
    private String username;

    @Value("${security.user.password}")
    private String password;

    @Autowired
    private Catalog catalog;


    @Given("^the user does not provides the header:$")
    public void theUserDoesNotProvideTheHeader(){
        restHelper.withHeader(BrokerApiVersion.DEFAULT_API_VERSION_HEADER, null);
        restHelper.withCredential(username, password);
    }

    @Given("^the user asks for version (.*)$")
    public void theUserAsksForVersion(String version){
        restHelper.withHeader(BrokerApiVersion.DEFAULT_API_VERSION_HEADER, version);
        restHelper.withCredential(username, password);
    }

    @When("^he requests the catalog$")
    public void heRequestsTheCatalog(){
        restHelper.get(buildUrl(CatalogController.BASE_PATH), Catalog.class);
    }

    @And("he gets the good catalog")
    public void heGetsTheCatalog(){
        Catalog remoteCatalog = restHelper.getBody(Catalog.class);
        assertThat(catalog.getServiceDefinitions().size(), is(equalTo(1)));
        assertThat(remoteCatalog.getServiceDefinitions().size(), is(equalTo(1)));
        assertThat(remoteCatalog.getServiceDefinitions().get(0).getId(), is(equalTo(catalog.getServiceDefinitions().get(0).getId())));

    }
}
