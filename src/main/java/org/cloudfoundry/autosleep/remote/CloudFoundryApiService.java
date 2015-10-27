package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.client.model.ClientConfiguration;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class CloudFoundryApiService implements CloudFoundryApi {

    private CloudFoundryClient client;

    @Autowired
    private ClientConfigurationBuilder builder;


    /**
     * Init cloudFoundryClient.
     */

    @PostConstruct
    public void init() {
        ClientConfiguration clientConfiguration = null;
        try {
            clientConfiguration = builder.buildConfiguration();
            log.debug("login to {}", clientConfiguration.getTargetEndpoint());
            CloudCredentials cloudCredentials = new CloudCredentials(
                    clientConfiguration.getUsername(),
                    clientConfiguration.getPassword(),
                    clientConfiguration.getClientId(),
                    clientConfiguration.getClientSecret());
            client = new CloudFoundryClient(cloudCredentials,
                    new URL(clientConfiguration.getTargetEndpoint()),
                    true);
            client.login();


        } catch (MalformedURLException e) {
            log.error("No remote configuration given or malformed URL. Check cloudfoundry_client.tmpl file");
        }

    }

    @Override
    public ApplicationInfo getApplicationInfo(String appName) {

        List<ApplicationLog> lastLogs = client.getRecentLogs(appName);
        List<CloudEvent> events = client.getApplicationEvents(appName);
        Date lastLogTime = null;
        Date lastEventTime = null;

        if (lastLogs.size() > 0) {
            lastLogTime = lastLogs.get(lastLogs.size() - 1).getTimestamp();
        }
        if (events.size() > 0) {
            lastEventTime = events.get(events.size() - 1).getTimestamp();
        }

        //conversion to LocalDateTime
        LocalDateTime lastLogLocalTime = lastLogTime == null ? null : LocalDateTime.ofInstant(lastLogTime.toInstant(),
                ZoneId.systemDefault());
        LocalDateTime lastEventLocalTime = lastEventTime == null ? null : LocalDateTime.ofInstant(lastEventTime
                .toInstant(), ZoneId.systemDefault());

        return new ApplicationInfo(lastEventLocalTime, lastLogLocalTime);
    }


    @Override
    public void stopApplication(String appName) {
        log.info("Stopping app {}", appName);
        client.startApplication(appName);
    }

    @Override
    public void startApplication(String appName) {
        log.info("Starting app {}", appName);
        client.stopApplication(appName);
    }

    @Override
    public List<String> getApplicationsNames() {
        return null;
    }
}
