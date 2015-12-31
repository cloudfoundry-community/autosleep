package org.cloudfoundry.autosleep.worker.remote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * Created by BUCE8373 on 31/12/2015.
 */
@Builder
@AllArgsConstructor
public class ClientHandler {

    @Getter
    private CloudFoundryClient client;

    private OAuth2AccessToken token;

    @Getter
    private String targetEndpoint;

    public OAuth2AccessToken getToken() {
        if (token.getExpiresIn() < 50) {
            /// 50 seconds before expiration? Then refresh it.
            token = client.login();
            return token;
        } else {
            return token;
        }
    }
}
