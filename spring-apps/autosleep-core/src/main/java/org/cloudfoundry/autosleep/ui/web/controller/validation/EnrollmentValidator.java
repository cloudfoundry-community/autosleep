package org.cloudfoundry.autosleep.ui.web.controller.validation;

import java.time.Duration;
import java.util.regex.Pattern;

import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.EnrollmentConfig;
import org.cloudfoundry.autosleep.ui.servicebroker.service.InvalidParameterException;
import org.cloudfoundry.autosleep.ui.servicebroker.service.parameters.ParameterReader;
import org.cloudfoundry.autosleep.ui.web.model.OrgEnrollmentConfigRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Configuration
public class EnrollmentValidator implements Validator {

    @Autowired
    private ParameterReader<Config.ServiceInstanceParameters.Enrollment> autoEnrollmentReader;

    @Autowired
    private ParameterReader<EnrollmentConfig.EnrollmentParameters.EnrollmentState> stateReader;

    @Autowired
    private ParameterReader<Pattern> excludeFromAutoEnrollmentReader;

    @Autowired
    private ParameterReader<Duration> idleDurationReader;

    public boolean supports(Class<?> clazz) {
        return OrgEnrollmentConfigRequest.class.equals(clazz);
    }

    public void validate(Object target, Errors errors) {
        OrgEnrollmentConfigRequest orgEnrollmentConfig = (OrgEnrollmentConfigRequest) target;
        consumeParameter("idleDuration", orgEnrollmentConfig.getIdleDuration(), false,
                idleDurationReader, errors);
        consumeParameter("excludeSpacesFromAutoEnrollment",
                orgEnrollmentConfig.getExcludeSpacesFromAutoEnrollment(), false,
                excludeFromAutoEnrollmentReader, errors);
        consumeParameter("autoEnrollment", orgEnrollmentConfig.getAutoEnrollment(), false,
                autoEnrollmentReader, errors);
        consumeParameter("state", orgEnrollmentConfig.getState(), false, stateReader, errors);
    }

    private <T> void consumeParameter(String attributeName, Object parameter, boolean withDefault,
            ParameterReader<T> reader, Errors errors) throws InvalidParameterException {
        try {
            reader.readParameter(parameter, withDefault);
        } catch (InvalidParameterException ipe) {
            errors.rejectValue(attributeName, attributeName + ":incorrect", ipe.getMessage());
        } catch (Exception e) {
            errors.reject(e.getMessage());
        }
    }

}
