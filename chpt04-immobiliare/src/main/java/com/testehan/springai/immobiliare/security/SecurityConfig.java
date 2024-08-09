package com.testehan.springai.immobiliare.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomerOAuth2UserService customerOAuth2UserService;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(CsrfConfigurer::disable);
        http.oauth2Login( oauth2Login -> oauth2Login
//                .loginPage("/login")
                .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                        .userService(customerOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler));
        http.authorizeHttpRequests(c -> c.anyRequest().authenticated());

        return http.build();
    }
}