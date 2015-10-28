package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.client.model.ClientConfiguration;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
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
import java.util.UUID;

@Slf4j
@Service
public class CloudFoundryApi implements CloudFoundryApiService {

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
    public ApplicationInfo getApplicationInfo(UUID appUid) {
        try {
            CloudApplication app = client.getApplication(appUid);
            if (app != null) {
                log.debug("Getting application info for app {}", app.getName());
                List<ApplicationLog> lastLogs = client.getRecentLogs(app.getName());
                List<CloudEvent> events = client.getApplicationEvents(app.getName());
                Date lastLogTime = null;
                Date lastEventTime = null;

                if (lastLogs.size() > 0) {
                    lastLogTime = lastLogs.get(lastLogs.size() - 1).getTimestamp();
                }
                if (events.size() > 0) {
                    lastEventTime = events.get(events.size() - 1).getTimestamp();
                }

                //conversion to LocalDateTime
                LocalDateTime lastLogLocalTime = lastLogTime == null ? null : LocalDateTime.ofInstant(lastLogTime
                                .toInstant(),
                        ZoneId.systemDefault());
                LocalDateTime lastEventLocalTime = lastEventTime == null ? null : LocalDateTime.ofInstant(lastEventTime
                        .toInstant(), ZoneId.systemDefault());
                return new ApplicationInfo(lastEventLocalTime, lastLogLocalTime);
            } else {
                log.error("No app found for UID {}", appUid);
            }
        } catch (Throwable t) {
            log.error("error", t);
        }
        return null;
    }


    @Override
    public void stopApplication(UUID appUid) {
        try {
            CloudApplication app = client.getApplication(appUid);
            if (app != null) {
                log.info("Stopping app {}", app.getName());
                client.stopApplication(app.getName());
            } else {
                log.error("No app found for UID {}", appUid);
            }
        } catch (Throwable t) {
            log.error("error", t);
        }
    }

    @Override
    public void startApplication(UUID appUid) {
        try {
            CloudApplication app = client.getApplication(appUid);
            if (app != null) {
                log.info("Starting app {}", app.getName());
                client.startApplication(app.getName());
            } else {
                log.error("No app found for UID {}", appUid);
            }
        } catch (Throwable t) {
            log.error("error", t);
        }
    }

    @Override
    public List<String> getApplicationsNames() {
        return null;
    }
}
