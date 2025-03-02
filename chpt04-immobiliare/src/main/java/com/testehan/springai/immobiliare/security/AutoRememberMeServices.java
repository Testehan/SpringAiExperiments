package com.testehan.springai.immobiliare.security;

import com.testehan.springai.immobiliare.model.auth.AuthenticationType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.log.LogMessage;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class AutoRememberMeServices extends PersistentTokenBasedRememberMeServices {

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final CustomerUserDetailsService userDetailsService;  // Repository to fetch the refresh token
    private final OAuth2AuthorizedClientService authorizedClientService;  // For token management
    private final MongoRememberMeTokenRepository rememberMeTokenRepository;

    public AutoRememberMeServices(String key, ClientRegistrationRepository clientRegistrationRepository, CustomerUserDetailsService userDetailsService, OAuth2AuthorizedClientService authorizedClientService,
                                  MongoRememberMeTokenRepository rememberMeTokenRepository) {
        super(key, userDetailsService, rememberMeTokenRepository);  // Super constructor to handle default behavior
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.userDetailsService = userDetailsService;
        this.authorizedClientService = authorizedClientService;
        setAlwaysRemember(true);        // the app does not have a "remember me" checkbox, so it will alays remember logins
        this.rememberMeTokenRepository = rememberMeTokenRepository;
    }

    @Override
    public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
        // Check the remember-me cookie (this is already handled by the parent class)
        Authentication authentication = null;
        try {
            authentication = super.autoLogin(request, response);
        } catch (CookieTheftException ex){
            logger.error(ex.getMessage());
        }

        // If the user is authenticated, try refreshing the access token using the refresh token
        if (authentication != null && authentication.isAuthenticated()) {
            RememberMeAuthenticationToken oauthToken = (RememberMeAuthenticationToken) authentication;

            var immobiliareUser = ((CustomerUserDetails) oauthToken.getPrincipal()).getImmobiliareUser();
            var email = immobiliareUser.getEmail();
            var refreshToken = getRefreshTokenFromDatabase(email);

            if (immobiliareUser.getAuthenticationType().equals(AuthenticationType.GOOGLE)) {

                if (refreshToken != null) {
                    // Use the refresh token to get a new access token
                    OAuth2AccessToken newAccessToken = refreshGoogleAccessToken(refreshToken);

                    if (newAccessToken != null) {
                        // Update the security context with the new access token
                        authenticateWithNewAccessToken(newAccessToken, email, AuthenticationType.GOOGLE);
                    }
                }
            }
            else {
                // facebook mechanism is different than googles for these refresh/longlived tokens
                var longLivedAccessToken = new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        refreshToken,
                        Instant.now(),
                        Instant.now().plusSeconds(1209600)      // 2 weeks
                );
                authenticateWithNewAccessToken(longLivedAccessToken, email, AuthenticationType.FACEBOOK);
            }

        }

        return authentication;
    }

    @Override
    protected void onLoginSuccess(HttpServletRequest request, HttpServletResponse response,
                                  Authentication successfulAuthentication) {

        String email = ((DefaultOAuth2User)successfulAuthentication.getPrincipal()).getAttribute("email");
        this.logger.debug(LogMessage.format("Creating new persistent login for user %s", email));
        var persistentToken = new PersistentRememberMeToken(email, generateSeriesData(), generateTokenData(), new Date());

        try {
            this.rememberMeTokenRepository.createNewToken(persistentToken);
            setCookie(new String[] { persistentToken.getSeries(), persistentToken.getTokenValue() }, getTokenValiditySeconds(), request, response);
        }
        catch (Exception ex) {
            this.logger.error("Failed to save persistent token ", ex);
        }
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            String email = "";
            if (principal instanceof DefaultOAuth2User) {
                email = ((DefaultOAuth2User) principal).getAttribute("email");
            }
            if (principal instanceof CustomerUserDetails){
                email = ((CustomerUserDetails) principal).getImmobiliareUser().getEmail();
            }
            this.rememberMeTokenRepository.removeUserTokens(email);
        }
        cancelCookie(request, response);
    }

    private String getRefreshTokenFromDatabase(String email) {
        return ((CustomerUserDetails) userDetailsService.loadUserByUsername(email)).getImmobiliareUser().getRefreshToken();
    }

    private OAuth2AccessToken refreshGoogleAccessToken(String refreshToken) {
        // Set up the token exchange URL for Google
        String tokenEndpoint = "https://oauth2.googleapis.com/token";

        ClientRegistration googleClientRegistration = clientRegistrationRepository.findByRegistrationId("google");

        // Prepare the request parameters
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);
        body.add("client_id", googleClientRegistration.getClientId());
        body.add("client_secret", googleClientRegistration.getClientSecret());

        // Create a RestTemplate and make the POST request
        RestTemplate restTemplate = new RestTemplate();
        try {
            // Send the POST request to get the new access token
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenEndpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(body, new HttpHeaders()),
                    Map.class
            );

            // Extract the new access token from the response body
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("access_token")) {
                String accessToken = (String) responseBody.get("access_token");
                String tokenType = (String) responseBody.get("token_type");
                Integer expiresIn = (Integer) responseBody.get("expires_in");

                // Return the new OAuth2AccessToken
                return new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER, //valueOf(tokenType.toUpperCase()),
                        accessToken,
                        Instant.now(),
                        Instant.now().plusSeconds(expiresIn)
                );
            }
        } catch (Exception e) {
            // Handle error (e.g., log or throw an exception)
            e.printStackTrace();
        }

        return null;  // Return null if unable to refresh the token
    }

    private void authenticateWithNewAccessToken(OAuth2AccessToken accessToken, String email, AuthenticationType authenticationType) {
        // Fetch the user by email
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (userDetails == null) {
            throw new UsernameNotFoundException("User with email " + email + " not found");
        }

        // Create a minimal principal
        OAuth2User principal = new DefaultOAuth2User(
                userDetails.getAuthorities(),
                Collections.singletonMap("email", email),
                "email"
        );

        // Create an OAuth2AuthenticationToken with the principal
        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
                principal,
                userDetails.getAuthorities(),
                authenticationType.toString().toLowerCase()
        );

        // Save the authorized client
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                clientRegistrationRepository.findByRegistrationId(authenticationType.toString().toLowerCase()), // Replace with your registration ID
                email,
                accessToken
        );

        authorizedClientService.saveAuthorizedClient(authorizedClient, authenticationToken);

        // Set the new authentication in the SecurityContextHolder
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

}