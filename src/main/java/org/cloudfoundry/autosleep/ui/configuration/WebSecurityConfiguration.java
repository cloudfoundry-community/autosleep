package org.cloudfoundry.autosleep.ui.configuration;

import org.cloudfoundry.autosleep.config.Config;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Created by buce8373 on 14/12/2015.
 */
@Configuration
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/**").authorizeRequests().anyRequest().authenticated();
        http.requestMatchers().antMatchers(Config.Path.dashboardContext + "/**",
                Config.Path.apiContext + Config.Path.servicesSubPath + "/*/applications/",
                "/css/**", "/fonts/**",
                "/javascript/**").and().authorizeRequests().anyRequest().anonymous();
    }
}
