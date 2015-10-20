package org.cloudfoundry.autosleep.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.cloudfoundry.autosleep.client.CloudFoundryException.Type;
import org.cloudfoundry.autosleep.client.model.AbstractEntity;
import org.cloudfoundry.autosleep.client.model.AppEntity;
import org.cloudfoundry.autosleep.client.model.ChangeAppRequest;
import org.cloudfoundry.autosleep.client.model.ClientConfiguration;
import org.cloudfoundry.autosleep.client.model.CloudfoundryObject;
import org.cloudfoundry.autosleep.client.model.CloudfoundryObjectList;
import org.cloudfoundry.autosleep.client.model.OAuthCredentials;
import org.cloudfoundry.autosleep.client.model.OrganizationEntity;
import org.cloudfoundry.autosleep.client.model.SpaceEntity;
import org.cloudfoundry.autosleep.client.model.TargetEndpointInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


@Service
@Slf4j
public class CloudFoundryApiClient implements CloudFoundryApiClientService {


    private TargetEndpointInformation endpointInformation;

    @Getter
    private OAuthCredentials credentials;

    private ClientConfiguration clientConfiguration;


    private RestTemplate restTemplate;


    @Autowired
    public CloudFoundryApiClient(ClientConfiguration clientConfiguration)
            throws CloudFoundryException {
        log.debug("setConfiguration - {}", clientConfiguration.getTargetEndpoint());
        try {
            HttpClientBuilder httpClientBuilder = HttpClients.custom().useSystemProperties();
            if (clientConfiguration.isEnableSelfSignedCertificates()) {
                log.debug("setConfiguration - enabling self signed certificates");
                httpClientBuilder.setSslcontext(new SSLContextBuilder().useSSL().loadTrustMaterial(null,
                        new TrustSelfSignedStrategy()).build());
                //httpClientBuilder.setHostnameVerifier(BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
            }
            HttpClient httpClient = httpClientBuilder.build();
            HttpComponentsClientHttpRequestFactory requestFactory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);
            this.restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(requestFactory);
            this.clientConfiguration = clientConfiguration;
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException n) {
            throw new CloudFoundryException(Type.Configuration, n);
        } catch (HttpClientErrorException h) {
            throw new CloudFoundryException(Type.CallError, h.getStatusCode().value(), h);
        }
    }

    @Override
    public void initCredential(String username, String password) throws CloudFoundryException {
        log.debug("initCredential - {}", username);
        if (this.endpointInformation == null) {
            this.endpointInformation = restTemplate.getForObject(clientConfiguration
                            .getTargetEndpoint() + "/v2/info",
                    TargetEndpointInformation.class);
        }
        credentials = postUrlEncodedForm(String.format("grant_type=password&username=%s&scope=&password=%s",
                username, password), OAuthCredentials.class);
    }

    @Override
    public void setCredentials(String refreshToken) throws CloudFoundryException {
        log.debug("setCredentials");
        refreshToken(refreshToken);
    }

    @Override
    public void logout() {
        this.credentials = null;
    }

    private static class OrganizationListResult extends CloudfoundryObjectList<OrganizationEntity> {
    }

    private static class SpaceListResult extends CloudfoundryObjectList<SpaceEntity> {
    }

    private static class AppListResult extends CloudfoundryObjectList<AppEntity> {
    }

    private static class AppResult extends CloudfoundryObject<AppEntity> {
    }


    @Override
    public void readOrganizations(CloudfoundryObjectReader<OrganizationEntity> reader) throws CloudFoundryException {
        checkInitialized();
        iterateOnPagedResources("/v2/organizations", reader, OrganizationListResult.class);
    }

    @Override
    public void readSpaces(CloudfoundryObject<OrganizationEntity> organizationEntity,
                           CloudfoundryObjectReader<SpaceEntity> reader) throws CloudFoundryException {
        checkInitialized();
        iterateOnPagedResources(organizationEntity.getEntity().getSpacesUrl(), reader, SpaceListResult.class);
    }

    @Override
    public void readApps(CloudfoundryObject<SpaceEntity> spaceEntity, CloudfoundryObjectReader<AppEntity> reader)
            throws CloudFoundryException {
        checkInitialized();
        iterateOnPagedResources(spaceEntity.getEntity().getAppsUrls(), reader, AppListResult.class);
    }

    @Override
    public CloudfoundryObject<AppEntity> startApp(CloudfoundryObject<AppEntity> app) throws CloudFoundryException {
        return changeAppState(app.getMetadata().getUrl(), "STARTED");
    }

    @Override
    public CloudfoundryObject<AppEntity> stopApp(CloudfoundryObject<AppEntity> app) throws CloudFoundryException {
        return changeAppState(app.getMetadata().getUrl(), "STOPPED");
    }

    private CloudfoundryObject<AppEntity> changeAppState(String applicationUrl, String newState) throws
            CloudFoundryException {
        checkInitialized();
        ChangeAppRequest request = new ChangeAppRequest();
        request.setState(newState);
        return invokeTargetEndpoint(applicationUrl + "?stage_async={async}",
                HttpMethod.PUT, request, AppResult.class, true);
    }

    private void refreshToken(String refreshToken) throws CloudFoundryException {
        log.debug("refreshToken");
        credentials = postUrlEncodedForm("grant_type=refresh_token&scope=&refresh_token=" + refreshToken,
                OAuthCredentials.class);
    }

    private <T> T postUrlEncodedForm(String form, Class<T> responseClass) throws CloudFoundryException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientConfiguration
                    .getClientId() + ":" + clientConfiguration.getClientSecret())
                    .getBytes(StandardCharsets.UTF_8)));
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<String> entity = new HttpEntity<>(form,
                    headers);
            ResponseEntity<T> responseEntity = restTemplate.postForEntity(
                    endpointInformation.getAuthorizationEndpoint() + "/oauth/token", entity, responseClass);
            return checkResponse(responseEntity);
        } catch (HttpClientErrorException h) {
            throw new CloudFoundryException(Type.CallError, h.getStatusCode().value(), h);
        }
    }


    private void checkInitialized() throws CloudFoundryException {
        if (credentials == null) {
            throw new CloudFoundryException(Type.Configuration, new NullPointerException("Client is not initialized"));
        }
    }


    private <T extends AbstractEntity> void iterateOnPagedResources(String url, CloudfoundryObjectReader<T> reader,
                                                                    Class<? extends CloudfoundryObjectList<T>>
                                                                            resultClass) throws CloudFoundryException {
        CloudfoundryObjectList<T> result = invokeTargetEndpoint(url, HttpMethod.GET, null, resultClass);
        boolean keepReading = true;
        while (keepReading) {
            for (CloudfoundryObject<T> entity : result.getResources()) {
                if (!reader.read(entity)) {
                    return;
                }
            }
            if (result.getNextPageUrl() != null) {
                result = invokeTargetEndpoint(result.getNextPageUrl(), HttpMethod.GET, null, resultClass);
            } else {
                keepReading = false;
            }

        }
    }

    private <B, R> R invokeTargetEndpoint(String url, HttpMethod method, B data,
                                          Class<R> responseClass, Object... uriVariables) throws CloudFoundryException {
        HttpHeaders headers = generateOAuthHeaders();
        HttpEntity<B> entity = new HttpEntity<>(data,
                headers);
        try {
            ResponseEntity<R> responseEntity = restTemplate.exchange(
                    clientConfiguration.getTargetEndpoint() + url, method, entity, responseClass, uriVariables);
            if (responseEntity.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new HttpClientErrorException(responseEntity.getStatusCode());
            } else {
                return checkResponse(responseEntity);
            }


        } catch (HttpClientErrorException h) {
            if (h.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.debug("attempting to refresh token a");
                refreshToken(credentials.getRefreshToken());
                log.debug("token refreshed");
                headers.add("Authorization", "Bearer " + credentials.getAccessToken());
                ResponseEntity<R> responseEntity = restTemplate.exchange(
                        clientConfiguration.getTargetEndpoint() + url, HttpMethod.GET, entity, responseClass);
                return checkResponse(responseEntity);
            } else {
                throw new CloudFoundryException(Type.CallError, h.getStatusCode().value(), h);
            }
        }
    }

    private HttpHeaders generateOAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + credentials.getAccessToken());

        return headers;
    }

    private <T> T checkResponse(ResponseEntity<T> responseEntity) throws CloudFoundryException {
        //only allow 2XX codes
        if (responseEntity.getStatusCode().value() / 100 == 2) {
            return responseEntity.getBody();
        } else {
            throw new CloudFoundryException(Type.CallError, responseEntity.getStatusCode().value(), new
                    HttpClientErrorException(responseEntity.getStatusCode()));
        }
    }


}
