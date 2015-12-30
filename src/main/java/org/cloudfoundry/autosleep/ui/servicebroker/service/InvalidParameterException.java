package org.cloudfoundry.autosleep.ui.servicebroker.service;

import lombok.Getter;
import org.springframework.http.converter.HttpMessageNotReadableException;

@Getter
public class InvalidParameterException extends HttpMessageNotReadableException {

    private String parameterName;

    public InvalidParameterException(String parameterName, String error) {
        super("'" + parameterName + "': " + error);
        this.parameterName = parameterName;
    }
}
