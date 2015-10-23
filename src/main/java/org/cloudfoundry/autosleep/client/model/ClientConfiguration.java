package org.cloudfoundry.autosleep.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientConfiguration {
    private String targetEndpoint;

    private boolean enableSelfSignedCertificates;

    private String clientId;

    private String clientSecret;
}
