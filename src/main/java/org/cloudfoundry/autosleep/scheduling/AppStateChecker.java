package org.cloudfoundry.autosleep.scheduling;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.remote.ApplicationActivity;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.util.LastDateComputer;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class AppStateChecker implements Runnable {

    private final UUID appUid;

    private final String bindingId;

    private final Duration period;

    private final CloudFoundryApiService remote;

    private final Clock clock;

    private BindingRepository bindingRepository;

    private ApplicationRepository appRepository;

    public void start() {
        clock.scheduleTask(bindingId, Duration.ofSeconds(0), this);
    }

    @Override
    public void run() {
        ApplicationInfo applicationInfo = remote.getApplicationInfo(appUid);
        Instant nextCheckTime = null;

                            if (nextIdleTime.isBefore(Instant.now())) {
                                log.info("Stopping app [{} / {}], last event: {}, last log: {}",
                                        applicationActivity.getName(), appUid,
                                        applicationActivity.getLastEvent(), applicationActivity.getLastLog());
                                remote.stopApplication(appUid);
                                nextCheckTime = rescheduleWithDefaultPeriod();
                            } else {
                                //rescheduled itself
                                Duration delta = Duration.between(Instant.now(), nextIdleTime);
                                nextCheckTime = reschedule(delta);
                            }
                        } else {
                            log.error("cannot find last event");
                            nextCheckTime = reschedule(period);
                        }

                    }
                } else {
                    log.debug("binding no longer exist. cancelling task");
                    clock.removeTask(bindingId);
                }

            } else {
                log.debug("failed to retrieve application activity informations");
                nextCheckTime = rescheduleWithDefaultPeriod();
            }
            applicationInfo.setNextCheck(nextCheckTime);
            appRepository.save(applicationInfo);
        } else {
            log.debug("application no longer exists. cancelling task");
            clock.removeTask(bindingId);
        }
    }

    protected Instant reschedule(Duration delta) {
        log.debug("Rescheduling in {}", delta.toString());
        clock.scheduleTask(bindingId, delta, this);
        return Instant.now().plus(delta);
    }

    protected Instant rescheduleWithDefaultPeriod() {
        return reschedule(period);
    }

}
