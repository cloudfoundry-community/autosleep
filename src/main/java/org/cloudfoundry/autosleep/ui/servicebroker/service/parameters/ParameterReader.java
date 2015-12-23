package org.cloudfoundry.autosleep.ui.servicebroker.service.parameters;

import org.cloudfoundry.autosleep.ui.servicebroker.service.InvalidParameterException;


public interface ParameterReader<T> {

    String getParameterName();

    T readParameter(Object parameter, boolean withDefault) throws InvalidParameterException;

}
