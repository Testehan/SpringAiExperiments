package com.testehan.springai.immobiliare.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    public static final int TWO_WEEKS_COOKIE_VALIDITY = 14 * 24 * 60 * 60;

    @Bean
    @Lazy
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(getUserDetailsService());
        authenticationManagerBuilder.authenticationProvider(getAuthenticationProvider());
        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        http.authorizeRequests()
                .requestMatchers("/images/**","/css/**","/webjars/**","/js/**")// to access these patterns a user can be NOT authenticated; ex in login page
                    .permitAll()
                .requestMatchers("/")
                    .authenticated()
                .anyRequest()
                    .permitAll()
                .and()
                .authenticationManager(authenticationManager)
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")     // because in spring security, the default login input parameter name is "username" and we use email
//                        .successHandler(databaseLoginSuccessHandler)
                        .permitAll())
//                .oauth2Login( oauth2Login -> oauth2Login
//                        .loginPage("/login")
//                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
//                                .userService(customerOAuth2UserService)
//                        )
//                        .successHandler(oAuth2LoginSuccessHandler)
//                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .permitAll())
                .rememberMe(rememberMe -> rememberMe.key("123456789").tokenValiditySeconds(TWO_WEEKS_COOKIE_VALIDITY));

        return http.build();
    }

    @Bean
    public UserDetailsService getUserDetailsService(){
        return new CustomerUserDetailsService();
    }

    public DaoAuthenticationProvider getAuthenticationProvider(){
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(getUserDetailsService());
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }

}
