package org.cloudfoundry.autosleep;

import cucumber.api.java.en.Then;

/**
 * Created by BUCE8373 on 13/10/2015.
 */
public class StatusCodeStepDefinition extends AbstractStep{
    @Then("^he gets a (\\d+) status code$")
    public void theClientReceivedStatusCode(int statusCode){
        checkLastStatusCode(statusCode);
    }

}
