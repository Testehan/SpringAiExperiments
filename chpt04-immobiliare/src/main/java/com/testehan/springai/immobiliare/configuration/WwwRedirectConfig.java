package com.testehan.springai.immobiliare.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class WwwRedirectConfig {

    @Value("${spring.profiles.active}")
    private String springActiveProfile;

    @Bean
    public FilterRegistrationBean<Filter> wwwRedirectFilter() {
        return new FilterRegistrationBean<>(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;
                String host = req.getHeader("Host");

                // we need this only in production
                if (host != null && host.equals("casamia.ai") && !springActiveProfile.equalsIgnoreCase("dev")) {
                    String redirectUrl = "https://www.casamia.ai" + req.getRequestURI();
                    String query = req.getQueryString();
                    if (query != null) {
                        redirectUrl += "?" + query;
                    }

                    res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                    res.setHeader("Location", redirectUrl);
                } else {
                    chain.doFilter(request, response);
                }
            }
        });
    }
}

