package org.cloudfoundry.autosleep.ui.web.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
@Slf4j
public class ApplicationLoggingConfig {
    private static int MAX_LOG_PAYLOAD_LENGTH = 1024;

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter result = new CommonsRequestLoggingFilter();
        result.setIncludeClientInfo(true);
        result.setIncludeQueryString(true);
        result.setIncludePayload(true);
        result.setMaxPayloadLength(MAX_LOG_PAYLOAD_LENGTH);
        return result;
    }
}
