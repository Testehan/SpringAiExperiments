package com.testehan.springai.immobiliare.configuration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CookieDomainConfig {

    @Value("${spring.profiles.active}")
    private String springActiveProfile;

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) {

                // we need this only in production
                if (!springActiveProfile.equalsIgnoreCase("dev")) {
                    SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
                    sessionCookieConfig.setDomain("casamia.ai");  // no leading dot!
                    sessionCookieConfig.setPath("/");
                    sessionCookieConfig.setHttpOnly(true);
                    sessionCookieConfig.setSecure(true); // Set true if HTTPS
                }
            }
        };
    }
}
