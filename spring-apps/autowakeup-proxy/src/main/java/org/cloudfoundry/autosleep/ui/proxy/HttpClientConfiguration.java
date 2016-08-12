/*
 * Autosleep
 * Copyright (C) 2016 Orange
 * Authors: Benjamin Einaudi   benjamin.einaudi@orange.com
 *          Arnaud Ruffin      arnaud.ruffin@orange.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.autosleep.ui.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Configuration
public class HttpClientConfiguration {

    @Value("${autowakeup.skip.ssl.validation:false}")
    private boolean skipSslValidation;

    private SSLContext buildSslContext(TrustManager trustManager) {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            return sslContext;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private TrustManager buildTrustAllCerts() {
        return new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] certificates, String client) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certificates, String client) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
    }

    private HostnameVerifier buildVerifyNoHostname() {
        return (hostname, session) -> true;
    }

    @Bean
    public RestTemplate restTemplate() {
        if (!skipSslValidation) {
            return new RestTemplate();
        } else {
            final HostnameVerifier hostnameVerifier = buildVerifyNoHostname();
            final SSLContext sslContext = buildSslContext(buildTrustAllCerts());
            return new RestTemplate(new SimpleClientHttpRequestFactory() {

                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection) {
                        HttpsURLConnection secureConnection = (HttpsURLConnection) connection;
                        secureConnection.setHostnameVerifier(hostnameVerifier);
                        secureConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            });
        }
    }

}
