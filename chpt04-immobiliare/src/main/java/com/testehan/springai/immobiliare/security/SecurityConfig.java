package com.testehan.springai.immobiliare.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@Order(10) // Lower numbers have higher precedence
public class SecurityConfig {

    @Value("${rememberme.token.key}")
    private String rememberMeKey;

    @Autowired
    private CustomerOAuth2UserService customerOAuth2UserService;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Autowired
    private CustomerUserDetailsService customerUserDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private MongoRememberMeTokenRepository mongoRememberMeTokenRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        OAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        LoggingAuthorizationRequestResolver customResolver = new LoggingAuthorizationRequestResolver(defaultResolver);

        http.csrf(CsrfConfigurer::disable);

        http
        .authorizeHttpRequests(req -> req
            .requestMatchers("/","/help","/blog","/contact", "/error**", "/error-login", "/login-modal",
                    "/reactivate","/confirmation","/accept-gdpr","/privacy-policy","/terms", "/s/**","/webhook/**").permitAll()
            .requestMatchers(antMatcher("/css/**")).permitAll()
            .requestMatchers(antMatcher("/js/**")).permitAll()
            .requestMatchers(antMatcher("/webjars/**")).permitAll()
            .requestMatchers(antMatcher("/images/**")).permitAll()
//                .requestMatchers(SecuredPaths.SECURED_URLS.toArray(new String[0])).authenticated()

                            // if this works, we don't need the above SECURED_URLs stuff because all other enpoints,
                            // except the ones permitted above, must be authenticated

            .anyRequest().authenticated()
        )
        .oauth2Login( oauth2Login -> oauth2Login
            .loginPage("/login-modal")
            .permitAll()
            .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                    .userService(customerOAuth2UserService))
            .successHandler(oAuth2LoginSuccessHandler())
            .authorizationEndpoint(authEndpoint -> authEndpoint.authorizationRequestResolver(customResolver))
        )

        .rememberMe(httpSecurityRememberMeConfigurer -> {
            httpSecurityRememberMeConfigurer.tokenValiditySeconds(1209600)  // Set token validity to 14 days (in seconds))
                    .key(rememberMeKey)
                    .rememberMeServices(autoRememberMeServices())
                    .useSecureCookie(false);
        })
        .logout((logout) -> logout.logoutSuccessUrl("/")        // after logout redirect user to /index
                                .invalidateHttpSession(true)
                                .clearAuthentication(true)
                                .deleteCookies("JSESSIONID"))
        .exceptionHandling(ex -> {
            ex.authenticationEntryPoint(customAuthenticationEntryPoint);
        });

        return http.build();
    }


    @Bean
    public RememberMeServices autoRememberMeServices() {
        return new AutoRememberMeServices(rememberMeKey, clientRegistrationRepository, customerUserDetailsService, authorizedClientService, mongoRememberMeTokenRepository);
    }

    @Bean
    public OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler() {
        return new OAuth2LoginSuccessHandler(userService, authorizedClientService);
    }

}