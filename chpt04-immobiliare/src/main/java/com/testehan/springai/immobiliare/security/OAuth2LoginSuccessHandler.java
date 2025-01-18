package com.testehan.springai.immobiliare.security;

import com.testehan.springai.immobiliare.model.auth.AuthenticationType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.util.AuthenticationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserService userService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuth2LoginSuccessHandler(UserService userService, OAuth2AuthorizedClientService authorizedClientService) {
        this.userService = userService;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        String userName = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("name");
        String userEmail = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("email");

        var userOptional = userService.getImmobiliareUserByEmail(userEmail);

        var authenticationType = AuthenticationUtil.getAuthenticationType(((OAuth2AuthenticationToken)authentication).getAuthorizedClientRegistrationId());

        if (userOptional.isEmpty()) {
            userService.addNewCustomerAfterOAuth2Login(userName, userEmail, authenticationType);
        } else {
            userService.updateAuthenticationType(userOptional.get(), authenticationType);
        }

        if (authenticationType.equals(AuthenticationType.GOOGLE)) {
            persistGoogleRefreshToken(authentication, userOptional);
        } else {
            persistFacebookRefreshToken(authentication, userOptional);
        }

        super.onAuthenticationSuccess(request, response, authentication);

    }

    private void persistGoogleRefreshToken(Authentication authentication, Optional<ImmobiliareUser> userOptional) {
        // Retrieve the OAuth2AuthorizedClient for the currently authenticated user
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String principalName = oauthToken.getPrincipal().getName();

        OAuth2AuthorizedClient authorizedClient = this.authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(), principalName);

        if (authorizedClient != null) {
            var refreshToken = authorizedClient.getRefreshToken();
            var refreshTokenValue = refreshToken != null ? refreshToken.getTokenValue(): null;

            if (refreshTokenValue != null) {
                // Store the refresh token in the backend (secure location)
                var user = userOptional.get();
                user.setRefreshToken(refreshTokenValue);
                userService.updateUser(user);
            }

        }

    }

    private void persistFacebookRefreshToken(Authentication authentication, Optional<ImmobiliareUser> userOptional) {
        // Retrieve the OAuth2AuthorizedClient for the currently authenticated user
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String principalName = oauthToken.getPrincipal().getName();

        OAuth2AuthorizedClient authorizedClient = this.authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(), principalName);

        if (authorizedClient != null) {
            OAuth2AccessToken shortLivedToken = authorizedClient.getAccessToken();

            // Exchange the short-lived token for a long-lived token
            String longLivedToken = exchangeForFacebookLongLivedToken(shortLivedToken.getTokenValue(), authorizedClient.getClientRegistration().getClientId(), authorizedClient.getClientRegistration().getClientSecret());

            // Store the long-lived token (e.g., in a session, database, or token store)
            if (longLivedToken != null) {
                // Store the refresh token in the backend (secure location)
                var user = userOptional.get();
                user.setRefreshToken(longLivedToken);
                userService.updateUser(user);
            }
        }
    }

    private String exchangeForFacebookLongLivedToken(String shortLivedToken, String fbClientId, String fbSecret) {
        // Build the URL to exchange the short-lived token for a long-lived token
        String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v12.0/oauth/access_token")
                .queryParam("grant_type", "fb_exchange_token")
                .queryParam("client_id", fbClientId)
                .queryParam("client_secret", fbSecret)
                .queryParam("fb_exchange_token", shortLivedToken)
                .toUriString();

        // Send HTTP request to Facebook and get the long-lived token
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("access_token")) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("Unable to get long-lived token from Facebook.");
        }
    }


}
