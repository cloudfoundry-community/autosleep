package org.cloudfoundry.autosleep;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.Resource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by BUCE8373 on 13/10/2015.
 */
@ContextConfiguration(classes = Application.class, loader = SpringApplicationContextLoader.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")//random port
@Slf4j
public class AbstractStep {

    @Value("${local.server.port}")   // access to the port used
    private int port;

    @Resource
    protected  RestHelper restHelper;


    protected void checkLastStatusCode(int statusCode){
        log.debug("checkLastStatusCode - {}", statusCode);
        assertThat(restHelper.getLastStatusCode(), is(equalTo(statusCode)));
    }

    protected String buildUrl(String path){ return "http://localhost:" + port + path;}
}
