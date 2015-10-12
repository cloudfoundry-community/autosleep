package com.orange.elpasso.autosleep;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;

/**
 * Created by buce8373 on 09/10/2015.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources"
        ,glue={"com.orange.elpasso.autosleep"}
)
public class RunCukesTest {
}
