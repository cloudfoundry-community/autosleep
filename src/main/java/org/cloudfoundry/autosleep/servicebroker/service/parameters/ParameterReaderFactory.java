package org.cloudfoundry.autosleep.servicebroker.service.parameters;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.servicebroker.service.InvalidParameterException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;


@Configuration
@Slf4j
public class ParameterReaderFactory {

    @Bean(name = Config.ServiceInstanceParameters.IDLE_DURATION)
    public ParameterReader<Duration> buildIdleDurationReader() {
        return new ParameterReader<Duration>() {
            @Override
            public String getParameterName() {
                return Config.ServiceInstanceParameters.IDLE_DURATION;
            }

            @Override
            public Duration readParameter(Map<String, Object> parameters, boolean withDefault) throws
                    InvalidParameterException {
                if (parameters.get(Config.ServiceInstanceParameters.IDLE_DURATION) != null) {
                    String inactivityPattern = (String) parameters.get(Config.ServiceInstanceParameters.IDLE_DURATION);
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

    @Bean(name = Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT)
    public ParameterReader<Pattern> buildExcludeFromAutoEnrollmentReader() {
        return new ParameterReader<Pattern>() {
            @Override
            public String getParameterName() {
                return Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT;
            }

            @Override
            public Pattern readParameter(Map<String, Object> parameters, boolean withDefault) throws
                    InvalidParameterException {
                if (parameters.get(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT) != null) {
                    String excludeNamesStr = (String) parameters.get(Config.ServiceInstanceParameters
                            .EXCLUDE_FROM_AUTO_ENROLLMENT);
                    if (!excludeNamesStr.trim().equals("")) {
                        log.debug("excludeFromAutoEnrollment " + excludeNamesStr);
                        try {
                            return Pattern.compile(excludeNamesStr);
                        } catch (PatternSyntaxException p) {
                            log.error("Wrong format for exclusion  - format cannot be compiled to a valid regexp");
                            throw new InvalidParameterException(Config.ServiceInstanceParameters
                                    .EXCLUDE_FROM_AUTO_ENROLLMENT,
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

    @Bean(name = Config.ServiceInstanceParameters.AUTO_ENROLLMENT)
    public ParameterReader<Config.ServiceInstanceParameters.Enrollment> buildAutoEnrollmentReader() {
        return new ParameterReader<Config.ServiceInstanceParameters.Enrollment>() {
            @Override
            public String getParameterName() {
                return Config.ServiceInstanceParameters.AUTO_ENROLLMENT;
            }

            @Override
            public Config.ServiceInstanceParameters.Enrollment readParameter(Map<String, Object> parameters, boolean
                    withDefault) throws
                    InvalidParameterException {
                if (parameters.get(Config.ServiceInstanceParameters.AUTO_ENROLLMENT) != null) {
                    String autoEnrollment = (String) parameters.get(Config.ServiceInstanceParameters.AUTO_ENROLLMENT);
                    log.debug("forcedAutoEnrollment " + autoEnrollment);
                    try {
                        return Config.ServiceInstanceParameters.Enrollment.valueOf(autoEnrollment);
                    } catch (IllegalArgumentException i) {
                        String availableValues = String.join(", ",
                                Arrays.asList(Config.ServiceInstanceParameters.Enrollment.values()).stream()
                                        .map(Config.ServiceInstanceParameters.Enrollment::name).collect(Collectors
                                        .toList()));
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


    @Bean(name = Config.ServiceInstanceParameters.SECRET)
    public ParameterReader<String> buildSecretReader() {

        return new ParameterReader<String>() {
            @Override
            public String getParameterName() {
                return Config.ServiceInstanceParameters.SECRET;
            }

            @Override
            public String readParameter(Map<String, Object> parameters, boolean withDefault) throws
                    InvalidParameterException {
                Object receivedSecret = parameters.get(Config.ServiceInstanceParameters.SECRET);
                if (receivedSecret != null) {
                    return String.class.cast(receivedSecret);
                } else {
                    return null;
                }
            }
        };
    }


}
