package com.testehan.springai.immobiliare.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final RequestMatcher securedEndpoints;

    public CustomAuthenticationEntryPoint() {
        // Convert the list of paths to individual AntPathRequestMatchers and pass them to OrRequestMatcher
        List<RequestMatcher> matchers = SecuredPaths.SECURED_URLS.stream()
                .map(AntPathRequestMatcher::new)
                .collect(Collectors.toList());

        // Now pass individual matchers using varargs
        this.securedEndpoints = new OrRequestMatcher(matchers.toArray(new RequestMatcher[0]));
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // Check if the request matches any of the secured paths
        if (securedEndpoints.matches(request)) {
            response.sendRedirect("/error-login");
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Page not found");
        }
    }

}
