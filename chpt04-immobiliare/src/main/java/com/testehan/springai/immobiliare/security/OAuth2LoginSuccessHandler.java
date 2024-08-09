package com.testehan.springai.immobiliare.security;

import com.testehan.springai.immobiliare.model.auth.AuthenticationType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        String userName = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("name");
        String userEmail = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("email");

        var user = userService.getImmobiliareUserByEmail(userEmail);

        var authenticationType = getAuthenticationType(((OAuth2AuthenticationToken)authentication).getAuthorizedClientRegistrationId());
        if (user == null){
            userService.addNewCustomerAfterOAuth2Login(userName, userEmail, authenticationType);
        } else {
            userService.updateAuthenticationType(user, authenticationType);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private AuthenticationType getAuthenticationType(String clientName){
        if (clientName.equalsIgnoreCase("google")){
            return AuthenticationType.GOOGLE;
        } else if (clientName.equalsIgnoreCase("facebook")){
            return AuthenticationType.FACEBOOK;
        }
        return AuthenticationType.DATABASE;
    }

}
