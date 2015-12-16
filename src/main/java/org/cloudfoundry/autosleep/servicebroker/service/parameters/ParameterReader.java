package org.cloudfoundry.autosleep.servicebroker.service.parameters;

import org.cloudfoundry.autosleep.servicebroker.service.InvalidParameterException;

import java.util.Map;


public interface ParameterReader<T> {

    String getParameterName();

    T readParameter(Map<String, Object> parameters, boolean withDefault) throws InvalidParameterException;

}
