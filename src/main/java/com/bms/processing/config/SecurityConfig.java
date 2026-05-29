package com.bms.processing.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/", true)
        );

        super.configure(http);
    }
}