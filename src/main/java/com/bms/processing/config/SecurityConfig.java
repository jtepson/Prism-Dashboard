package com.bms.processing.config;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
public class SecurityConfig {

        @Bean
        public OidcUserService oidcUserService() {
                OidcUserService delegate = new OidcUserService();

                return new OidcUserService() {
                        @Override
                        public OidcUser loadUser(OidcUserRequest userRequest) {
                                OidcUser oidcUser = delegate.loadUser(userRequest);

                                Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());

                                List<String> groups = oidcUser.getClaimAsStringList("groups");

                                if (groups != null) {
                                groups.forEach(group ->
                                        authorities.add(new SimpleGrantedAuthority(
                                                "GROUP_" + group.toUpperCase()
                                        ))
                                );
                                }

                                Map<String, Object> realmAccess = oidcUser.getClaim("realm_access");

                                if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
                                roles.forEach(role ->
                                        authorities.add(new SimpleGrantedAuthority(
                                                "ROLE_" + role.toString().toUpperCase()
                                        ))
                                );
                                }

                                return new DefaultOidcUser(
                                        authorities,
                                        oidcUser.getIdToken(),
                                        oidcUser.getUserInfo(),
                                        "preferred_username"
                                );
                        }
                };
        }

        @Bean
        @Order(1)
        public SecurityFilterChain secureShareSecurityFilterChain(
                        HttpSecurity http
                ) throws Exception {

                // allow external secure share flow without Keycloak
                http.securityMatcher("/share/**");

                http.authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll()
                );

                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http
                ) throws Exception {

                http.oauth2Login(oauth2 ->
                        oauth2.userInfoEndpoint(userInfo ->
                                userInfo.oidcUserService(oidcUserService())
                        )
                );

                http.with(
                        VaadinSecurityConfigurer.vaadin(),
                        vaadin ->
                                vaadin.oauth2LoginPage(
                                        "/oauth2/authorization/keycloak"
                                )
                );

                return http.build();
        }
}