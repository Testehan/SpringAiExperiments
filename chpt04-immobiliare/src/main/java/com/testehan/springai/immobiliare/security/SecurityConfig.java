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

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(CsrfConfigurer::disable);
        http.authorizeHttpRequests(req -> req
                .requestMatchers("/chat", "/respond", "/message","/favourites","/add","/edit/**","/api/apartments/**",
                        "/actuator/**","/api/user/**","/profile","/view/**").authenticated()
                .requestMatchers("/","/help","/blog","/contact", "/error/**", "/error-login", "/login-modal", "/reactivate","/confirmation").permitAll()
                .requestMatchers(antMatcher("/css/**")).permitAll()
                .requestMatchers(antMatcher("/js/**")).permitAll()
                .requestMatchers(antMatcher("/webjars/**")).permitAll()
                .requestMatchers(antMatcher("/images/**")).permitAll()
                )
            .oauth2Login( oauth2Login -> oauth2Login
                .loginPage("/login-modal")
                .permitAll()
                .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                        .userService(customerOAuth2UserService))
                .successHandler(oAuth2LoginSuccessHandler))
            .logout((logout) -> logout.logoutSuccessUrl("/")        // after logout redirect user to /index
                                    .invalidateHttpSession(true)
                                    .clearAuthentication(true)
                                    .deleteCookies("JSESSIONID"))
            .exceptionHandling(ex -> {
                ex.authenticationEntryPoint(customAuthenticationEntryPoint);
            });

        return http.build();
    }
}