package org.cloudfoundry.autosleep;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.community.servicebroker.controller.CatalogController;
import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


/**
 * Created by BUCE8373 on 13/10/2015.
 */
@Slf4j
public class BasicAuthStepDefinitions extends AbstractStep{
    @Value("${security.user.name}")
    private String username;

    @Value("${security.user.password}")
    private String password;

    @Autowired
    private BrokerApiVersion apiVersion;

    @Given("^the user does not provide authentication:$")
    public void userDoesNotProvideAuthentication(){
        restHelper.withHeader(BrokerApiVersion.DEFAULT_API_VERSION_HEADER, apiVersion.getApiVersion());
        restHelper.withCredential(null, null);
    }


    @Given("^the user provides bad authentication:$")
    public void userProvidesBadAuthentication(){
        restHelper.withHeader(BrokerApiVersion.DEFAULT_API_VERSION_HEADER, apiVersion.getApiVersion());
        restHelper.withCredential(username, password + "123");
    }

    @Given("^the user provides good authentication:$")
    public void userProvidesGoodAuthentication(){
        restHelper.withHeader(BrokerApiVersion.DEFAULT_API_VERSION_HEADER, apiVersion.getApiVersion());
        restHelper.withCredential(username, password);
    }


    @When("^he requests application$")
    public void heRequestsSomeurl(){
        restHelper.get(buildUrl(CatalogController.BASE_PATH), Catalog.class);
    }


}
