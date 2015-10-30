package org.cloudfoundry.autosleep.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.remote.CloudFoundryApiService;
import org.cloudfoundry.autosleep.repositories.BindingRepository;
import org.cloudfoundry.autosleep.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.servicebroker.model.AutoSleepServiceBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class GlobalWatcher {

    /**
     * An ID used to define wich instance of Autosleep application started the watch.
     */
    private final UUID watcherId;

    private Clock clock;

    private CloudFoundryApiService remote;

    private BindingRepository bindingRepository;

    private ServiceRepository serviceRepository;

    @Autowired
    public GlobalWatcher(Clock clock, CloudFoundryApiService remote, BindingRepository bindingRepository,
                         ServiceRepository serviceRepository) {
        this.clock = clock;
        this.remote = remote;
        this.bindingRepository = bindingRepository;
        this.serviceRepository = serviceRepository;
        this.watcherId = UUID.randomUUID();
    }

    @PostConstruct
    public void init() {
        log.debug("Initializer watchers for every app already bound (except if handle by another instance of "
                + "autosleep)");
        Iterable<AutoSleepServiceBinding> bindings = bindingRepository.findAll();
        bindings.forEach(binding -> {
            //check if the binding is not already handled by another instance of autosleep
            if (binding.getAssociatedWatcher() == null) {
                watchApp(binding);
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        log.debug("Canceling every watcher before shutdown");
        Iterable<AutoSleepServiceBinding> bindings = bindingRepository.findAll();
        bindings.forEach(binding -> {
            //check if the binding is handle by this instance of autosleep
            if (watcherId.equals(binding.getAssociatedWatcher())) {
                cancelWatch(binding);
            }
        });
    }

    public void watchApp(AutoSleepServiceBinding binding) {
        if (binding.getAssociatedWatcher() != null) {
            log.error("PROBABLE BUG. Binding already associated to another instance. This should be investigated.");
            return;
        }
        watchApp(binding.getId(), UUID.fromString(binding.getAppGuid()), serviceRepository.findOne(binding
                .getServiceInstanceId()).getInterval());
        binding.setAssociatedWatcher(watcherId);
        bindingRepository.save(binding);
    }

    private void watchApp(String taskId, UUID app, Duration interval) {
        log.debug("Initializing a watch on app {}, for an interval of {} ", app.toString(), interval.toString());
        AppStateChecker checker = new AppStateChecker(app,
                taskId,
                interval,
                remote,
                clock);
        checker.start();
    }

    public void cancelWatch(AutoSleepServiceBinding binding) {
        if (binding != null && binding.getId() != null) {
            clock.stopTimer(binding.getId());
            binding.setAssociatedWatcher(null);
            bindingRepository.save(binding);
        } else {
            log.error("PROBABLE BUG. Trying to cancel an unknown task... This has to be investigated. {}", binding);
        }

    }

}
