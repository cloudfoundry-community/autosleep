package org.cloudfoundry.autosleep.remote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URL;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientConfiguration {
    private URL targetEndpoint;

    private boolean enableSelfSignedCertificates;

    private String clientId;

    private String clientSecret;

    private String username;

    private String password;
}
