package org.cloudfoundry.autosleep;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Created by buce8373 on 14/10/2015.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest("server.port:0")//random port
@Slf4j
public abstract class AbstractRestTest {
    @Value("${local.server.port}")   // access to the port used
    private int port;

    @Value("${security.user.name}")
    protected String username;

    @Value("${security.user.password}")
    protected String password;


    protected static class Call<B, R> {
        private HttpHeaders headers = new HttpHeaders();

        private B data;

        private TestRestTemplate caller;

        private Class<R> responseClass;

        private String url;

        private HttpMethod method;

        private Call(TestRestTemplate caller, String url, HttpMethod method, B data, Class<R> responseClass) {
            this.caller = caller;
            this.url = url;
            this.method = method;
            this.data = data;
            this.responseClass = responseClass;
        }

        public Call<B, R> withHeader(String key, String value) {
            headers.add(key, value);
            return this;
        }

        public Call<B, R> withBasicAuthentication(String username, String password) {
            headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password)
                    .getBytes(StandardCharsets.UTF_8)));
            return this;
        }

        public ResponseEntity<R> call() {
            return caller.exchange(url, method, new HttpEntity<>(data, headers), responseClass);
        }
    }

    private TestRestTemplate caller = new TestRestTemplate();


    protected <B, R> Call<B, R> prepare(String path, HttpMethod method, B data, Class<R> responseClass) {
        return new Call<>(caller, buildUrl(path), method, data, responseClass);
    }

    private String buildUrl(String path) {
        return "http://localhost:" + port + path;
    }

}
