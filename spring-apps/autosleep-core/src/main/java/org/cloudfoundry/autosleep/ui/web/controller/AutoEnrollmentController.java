package org.cloudfoundry.autosleep.ui.web.controller;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    private ParameterReader<Config.ServiceInstanceParameters.Enrollment> autoEnrollmentReader;

    @Autowired
    private ParameterReader<Pattern> excludeFromAutoEnrollmentReader;

    @Autowired
    private ParameterReader<Duration> idleDurationReader;

    @Autowired
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
        orgEnrollmentRepository.delete(organizationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "{organizationId}")
    public ResponseEntity<OrgEnrollmentConfig> enrolOrganization(
            @PathVariable String organizationId,
            @RequestBody OrgEnrollmentConfigRequest orgEnrollerConfigRequest, BindingResult result)
            throws CloudFoundryException, BindException {

        log.debug("enrolOrganization - {}", organizationId);
        if (!cloudfoundryApi.isValidOrganization(organizationId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        validator.validate(orgEnrollerConfigRequest, result);
        if (result.hasErrors()) {
            throw new BindException(result);
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

        boolean isAlreadyRegistered = orgEnrollmentRepository.exists(organizationId);
        orgEnrollerConfig = orgEnrollmentRepository.save(orgEnrollerConfig);

        if (isAlreadyRegistered) {
            return new ResponseEntity<OrgEnrollmentConfig>(orgEnrollerConfig, HttpStatus.OK);
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location",
                    EnrollmentConfig.Path.ORG_AUTO_ENROLMENT_BASE_PATH + organizationId);
            return new ResponseEntity<OrgEnrollmentConfig>(orgEnrollerConfig, headers,
                    HttpStatus.CREATED);
        }
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
