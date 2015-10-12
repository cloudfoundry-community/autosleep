package org.cloudfoundry.autosleep;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Created by buce8373 on 09/10/2015.
 */

@ContextConfiguration(classes = Application.class, loader = SpringApplicationContextLoader.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")//random port
@Slf4j
public class StepDefinitions {

    @Value("${local.server.port}")   // access to the port used
    private int port;


    @Given("^the user has something:$")
    public void theUserHasSomething(){
        log.debug("theUserHasSomething");
    }

    @When("^something happens$")
    public void somethingHappens(){
        log.debug("somethingHappens");
    }

    @Then("^something else happens$")
    public void somethingElseHappens(){
        log.debug("somethingElseHappens");
    }

    @And("^the user has (\\d+) opened eyes$")
    public void theUserHasXOpenedEyes(int nbOpenedEyes){
        log.debug("theUserHasXOpenedEyes - {}", nbOpenedEyes);
        assertThat(nbOpenedEyes, is (equalTo(2)));
    }

}
