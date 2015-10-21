package org.cloudfoundry.autosleep.remote;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class RemoteMock implements IRemote{

    @Override
    public ApplicationInfo getApplicationInfo(String appGuid) {
        return new ApplicationInfo(LocalDateTime.now().minus(Duration.ofSeconds(5)),
                LocalDateTime.now().minus(Duration.ofSeconds(30)));
    }

    @Override
    public boolean stopApplication(String appGuid) {
        log.info("I iz stopping app {}",appGuid);
        return true;
    }

    @Override
    public boolean startApplication(String appGuid) {
        log.info("I iz starting app {}",appGuid);
        return true;
    }

    @Override
    public List<String> getApplicationsNames() {
        return null;
    }
}
