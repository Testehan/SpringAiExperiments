package com.testehan.springai.immobiliare.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomerOAuth2UserService customerOAuth2UserService;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(CsrfConfigurer::disable);

        http.authorizeHttpRequests(req -> req
                .requestMatchers(antMatcher("/css/**")).permitAll()
                .requestMatchers(antMatcher("/images/**")).permitAll()
                .anyRequest().authenticated())
            .oauth2Login( oauth2Login -> oauth2Login
                .loginPage("/login")
                .permitAll()
                .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                        .userService(customerOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler));

        return http.build();
    }
}