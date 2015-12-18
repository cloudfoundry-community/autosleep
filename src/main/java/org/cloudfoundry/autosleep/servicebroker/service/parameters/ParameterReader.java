package org.cloudfoundry.autosleep.servicebroker.service.parameters;

import org.cloudfoundry.autosleep.servicebroker.service.InvalidParameterException;


public interface ParameterReader<T> {

    String getParameterName();

    T readParameter(Object parameter, boolean withDefault) throws InvalidParameterException;

}
