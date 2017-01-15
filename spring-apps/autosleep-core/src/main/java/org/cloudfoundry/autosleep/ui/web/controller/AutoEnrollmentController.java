package org.cloudfoundry.autosleep.ui.web.controller;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryApiService;
import org.cloudfoundry.autosleep.access.cloudfoundry.CloudFoundryException;
import org.cloudfoundry.autosleep.access.dao.model.OrgEnrollmentConfig;
import org.cloudfoundry.autosleep.access.dao.repositories.OrgEnrollmentConfigRepository;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.config.EnrollmentConfig;
import org.cloudfoundry.autosleep.ui.servicebroker.service.parameters.ParameterReader;
import org.cloudfoundry.autosleep.ui.web.controller.validation.EnrollmentValidator;
import org.cloudfoundry.autosleep.ui.web.model.OrgEnrollmentConfigRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping(EnrollmentConfig.Path.ORG_AUTO_ENROLMENT_BASE_PATH)
@RestController
public class AutoEnrollmentController {

    @Autowired
    private OrgEnrollmentConfigRepository orgEnrollmentRepository;

    @Autowired
    private EnrollmentValidator validator;

    @Autowired
    private CloudFoundryApiService cloudfoundryApi;

    @Autowired
    @Qualifier(Config.ServiceInstanceParameters.AUTO_ENROLLMENT)
    private ParameterReader<Config.ServiceInstanceParameters.Enrollment> autoEnrollmentReader;

    @Autowired
    @Qualifier(Config.ServiceInstanceParameters.EXCLUDE_FROM_AUTO_ENROLLMENT)
    private ParameterReader<Pattern> excludeFromAutoEnrollmentReader;

    @Autowired
    @Qualifier(Config.ServiceInstanceParameters.IDLE_DURATION)
    private ParameterReader<Duration> idleDurationReader;

    @Autowired
    @Qualifier(EnrollmentConfig.EnrollmentParameters.STATE)
    private ParameterReader<EnrollmentConfig.EnrollmentParameters.EnrollmentState> stateReader;

    @RequestMapping(method = RequestMethod.GET, value = "/{organizationId}")
    public ResponseEntity<OrgEnrollmentConfig> getEnrolledOrganization(
            @PathVariable String organizationId) {
        log.debug("getEnrolledOrganization - {}", organizationId);
        OrgEnrollmentConfig orgConfig = orgEnrollmentRepository.findOne(organizationId);
        if (orgConfig == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<OrgEnrollmentConfig>(orgConfig, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{organizationId}")
    public ResponseEntity<OrgEnrollmentConfig> deleteOrganization(
            @PathVariable String organizationId) {
        log.debug("deleteEnrolledOrganization - {}", organizationId);

        OrgEnrollmentConfig orgConfig = orgEnrollmentRepository.findOne(organizationId);
        if (orgConfig == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        orgConfig.setState(
                EnrollmentConfig.EnrollmentParameters.EnrollmentState.backoffice_opted_out);
        orgConfig = orgEnrollmentRepository.save(orgConfig);
        return new ResponseEntity<OrgEnrollmentConfig>(orgConfig, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "{organizationId}")
    public ResponseEntity<OrgEnrollmentConfig> enrolOrganization(
            @PathVariable String organizationId,
            @Valid @RequestBody OrgEnrollmentConfigRequest orgEnrollerConfigRequest,
            BindingResult result) throws CloudFoundryException, BindException {

        log.debug("enrolOrganization - {}", organizationId);

        if (result.hasErrors()) {
            throw new BindException(result);
        }

        if (!cloudfoundryApi.isValidOrganization(organizationId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (orgEnrollmentRepository.exists(organizationId)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.ALLOW, RequestMethod.PATCH.name());
            return new ResponseEntity<>(headers, HttpStatus.METHOD_NOT_ALLOWED);
        }

        OrgEnrollmentConfig orgEnrollerConfig = OrgEnrollmentConfig.builder()
                .organizationGuid(organizationId)
                .idleDuration(orgEnrollerConfigRequest.getIdleDuration() == null ? null
                        : idleDurationReader
                                .readParameter(orgEnrollerConfigRequest.getIdleDuration(), false))
                .excludeSpacesFromAutoEnrollment(
                        orgEnrollerConfigRequest.getExcludeSpacesFromAutoEnrollment() == null ? null
                                : excludeFromAutoEnrollmentReader
                                        .readParameter(
                                                orgEnrollerConfigRequest
                                                        .getExcludeSpacesFromAutoEnrollment(),
                                                false))
                .autoEnrollment(orgEnrollerConfigRequest.getAutoEnrollment() == null ? null
                        : autoEnrollmentReader
                                .readParameter(orgEnrollerConfigRequest.getAutoEnrollment(), false))
                .state(orgEnrollerConfigRequest.getState() == null ? null
                        : stateReader.readParameter(orgEnrollerConfigRequest.getState(), false))
                .build();

        orgEnrollmentRepository.save(orgEnrollerConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location",
                EnrollmentConfig.Path.ORG_AUTO_ENROLMENT_BASE_PATH + organizationId);
        return new ResponseEntity<OrgEnrollmentConfig>(orgEnrollerConfig, headers,
                HttpStatus.CREATED);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(validator);
    }

    @ExceptionHandler({ CloudFoundryException.class })
    public ResponseEntity<Object> handleCloudFoundryException(CloudFoundryException cfe,
            HttpServletResponse response) {
        return new ResponseEntity<>(cfe, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ BindException.class })
    public ResponseEntity<List<ObjectError>> handleBindException(BindException be,
            HttpServletResponse response) {
        return new ResponseEntity<List<ObjectError>>(be.getAllErrors(), HttpStatus.BAD_REQUEST);
    }
}
