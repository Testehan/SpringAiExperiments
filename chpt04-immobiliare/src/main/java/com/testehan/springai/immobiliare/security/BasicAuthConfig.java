package com.testehan.springai.immobiliare.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@Order(1) // Lower numbers have higher precedence
public class BasicAuthConfig {

    @Value("${spring.security.user.name}")
    private String basicAuthUsername;

    @Value("${spring.security.user.password}")
    private String basicAuthPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService basicAuthUserDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username(basicAuthUsername)
                .password(passwordEncoder.encode(basicAuthPassword)) // Encode the password!
                .roles("ADMIN") // Assign a role (you can customize this)
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain securityFilterChainBasicAuth(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**","/agent/**") // Apply this chain only to /actuator paths
            .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/actuator/**","/agent/**").hasRole("ADMIN") // Admin role for Prometheus and agents
                            .requestMatchers("/actuator/**","/agent/**").authenticated()

            )
            .httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider basicAuthenticationProvider(PasswordEncoder passwordEncoder, UserDetailsService basicAuthUserDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(basicAuthUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

}
