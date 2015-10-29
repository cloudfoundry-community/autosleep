package org.cloudfoundry.autosleep.admin.controller;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.ClientConfiguration;
import org.cloudfoundry.autosleep.remote.CloudFoundryApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/configuration")
@Slf4j
public class ConfigurationController {

    @Autowired
    private CloudFoundryApi client;


    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView getConfigurationView() {
        log.debug("getConfigurationView - rendering view");
        Map<String, Object> parameters = new HashMap<>();
        ClientConfiguration clientConfiguration = null;
        if (client.getClientConfiguration() != null) {
            log.debug("getConfigurationView - client is configured");
            clientConfiguration = new ClientConfiguration(client.getClientConfiguration().getTargetEndpoint(),
                    client.getClientConfiguration().isEnableSelfSignedCertificates(),
                    client.getClientConfiguration().getClientId(), "",
                    client.getClientConfiguration().getUsername(), client.getClientConfiguration().getPassword());
        }
        parameters.put("clientConfiguration", clientConfiguration);
        return new ModelAndView("views/admin/configuration", parameters);
    }


    @RequestMapping(value = "/", method = RequestMethod.PUT)
    public ResponseEntity<String> updateCredentials(@RequestBody ClientConfiguration clientConfiguration) {
        log.debug("updateCredentials - {}", clientConfiguration.getTargetEndpoint());
        try {
            client.setClientConfiguration(clientConfiguration);
            return new ResponseEntity<>("", HttpStatus.OK);
        } catch (Throwable t) {
            return new ResponseEntity<>("Bad information provided: " + t.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(value = "/", method = RequestMethod.DELETE)
    public ResponseEntity<String> cleanCredentials() {
        log.debug("cleanCredentials");
        client.setClientConfiguration(null);
        return new ResponseEntity<>("", HttpStatus.NO_CONTENT);
    }

}
