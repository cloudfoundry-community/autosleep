package org.cloudfoundry.autosleep.ui.servicebroker.service.parameters;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.ui.servicebroker.service.InvalidParameterException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class ParameterReaderFactory {

    @Bean(name = Config.ServiceInstanceParameters.AUTO_ENROLLMENT)
    public ParameterReader<Config.ServiceInstanceParameters.Enrollment> buildAutoEnrollmentReader() {
        return new ParameterReader<Config.ServiceInstanceParameters.Enrollment>() {

            @Override
            public String getParameterName() {
                return Config.ServiceInstanceParameters.AUTO_ENROLLMENT;
            }

            @Override
            public Config.ServiceInstanceParameters.Enrollment readParameter(Object parameter,
                                                                             boolean withDefault)
                    throws InvalidParameterException {
                if (parameter != null) {
                    String autoEnrollment = (String) parameter;
                    log.debug("forcedAutoEnrollment " + autoEnrollment);
                    try {
                        return Config.ServiceInstanceParameters.Enrollment.valueOf(autoEnrollment);
                    } catch (IllegalArgumentException i) {
                        String availableValues = String.join(", ",
                                Arrays.asList(Config.ServiceInstanceParameters.Enrollment.values()).stream()
                                        .map(Config.ServiceInstanceParameters.Enrollment::name)
                                        .collect(Collectors.toList()));
                        log.error("Wrong value for auto enrollment  - choose one between [{}]", availableValues);
                        throw new InvalidParameterException(Config.ServiceInstanceParameters.AUTO_ENROLLMENT,
                                "choose one between: " + availableValues);
                    }
                } else if (withDefault) {
                    return Config.ServiceInstanceParameters.Enrollment.standard;
                } else {
                    return null;
                }
            }
        };
    }

    @Bean(name = Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT)
    public ParameterReader<Pattern> buildExcludeFromAutoEnrollmentReader() {
        return new ParameterReader<Pattern>() {

            @Override
            public String getParameterName() {
                return Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT;
            }

            @Override
            public Pattern readParameter(Object parameter, boolean withDefault) throws
                    InvalidParameterException {
                if (parameter != null) {
                    String excludeNamesStr = (String) parameter;
                    if (!excludeNamesStr.trim().equals("")) {
                        log.debug("excludeFromAutoEnrollment " + excludeNamesStr);
                        try {
                            return Pattern.compile(excludeNamesStr);
                        } catch (PatternSyntaxException p) {
                            log.error("Wrong format for exclusion  - format cannot be compiled to a valid regexp");
                            throw new InvalidParameterException(
                                    Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT,
                                    "should be a valid regexp");
                        }
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        };
    }

    @Bean(name = Config.ServiceInstanceParameters.IDLE_DURATION)
    public ParameterReader<Duration> buildIdleDurationReader() {
        return new ParameterReader<Duration>() {

            @Override
            public String getParameterName() {
                return Config.ServiceInstanceParameters.IDLE_DURATION;
            }

            @Override
            public Duration readParameter(Object parameter, boolean withDefault) throws
                    InvalidParameterException {
                if (parameter != null) {
                    String inactivityPattern = (String) parameter;
                    log.debug("pattern " + inactivityPattern);
                    try {
                        return Duration.parse(inactivityPattern);
                    } catch (DateTimeParseException e) {
                        log.error("Wrong format for inactivity duration - format should respect ISO-8601 duration "
                                + "format "
                                + "PnDTnHnMn");
                        throw new InvalidParameterException(Config.ServiceInstanceParameters.IDLE_DURATION,
                                "param badly formatted (ISO-8601). Example: \"PT15M\" for 15mn");
                    }
                } else if (withDefault) {
                    return Config.DEFAULT_INACTIVITY_PERIOD;
                } else {
                    return null;
                }
            }

        };
    }

    @Bean(name = Config.ServiceInstanceParameters.SECRET)
    public ParameterReader<String> buildSecretReader() {

        return new ParameterReader<String>() {

            @Override
            public String getParameterName() {
                return Config.ServiceInstanceParameters.SECRET;
            }

            @Override
            public String readParameter(Object parameter, boolean withDefault) throws
                    InvalidParameterException {
                if (parameter != null) {
                    return String.class.cast(parameter);
                } else {
                    return null;
                }
            }
        };
    }

}
