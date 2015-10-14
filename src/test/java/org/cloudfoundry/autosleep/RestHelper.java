package org.cloudfoundry.autosleep;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Created by BUCE8373 on 13/10/2015.
 */
@Service
@Slf4j
public class RestHelper {


    private String username;


    private String password;


    private RestTemplate restTemplate = new TestRestTemplate();

    private HttpHeaders headers = new HttpHeaders();

    private ResponseEntity<?> lastResponse;

    public void withCredential(String username, String password) {
        log.debug("withCredential - {} - {}", username, password);
        if ((username == null || password == null) && this.username != null) {
            log.debug("withCredential - cleaning auth");
            this.restTemplate = new TestRestTemplate();
            this.username = this.password = null;
        } else if (username != null && password != null && (!username.equals(this.username) || !password.equals(this.password))) {
            log.debug("withCredential - setting auth");
            this.restTemplate = new TestRestTemplate(username, password);
            this.username = username;
            this.password = password;
        }
    }

    public void withHeader(String key, String value) {
        log.debug("withHeader - {} - {}", key, value);
        if (value == null)
            headers.remove(key);
        else
            headers.set(key, value);
    }


    public void get(String url, Class<?> responseClass) {
        lastResponse = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Void>(null, headers), responseClass);
    }


    public int getLastStatusCode() {
        return lastResponse.getStatusCode().value();
    }

    public <T> T getBody(Class<T> responseClass) {
        return responseClass.cast(lastResponse.getBody());
    }
}
