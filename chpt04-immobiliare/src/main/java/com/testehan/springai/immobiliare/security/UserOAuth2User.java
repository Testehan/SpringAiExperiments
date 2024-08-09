package com.testehan.springai.immobiliare.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class UserOAuth2User implements OAuth2User {

    private OAuth2User oAuth2User;
    private String clientName;          // this will be either "google" or "facebook", so it helps differentiate between these 2
    private String fullName;

    public UserOAuth2User(OAuth2User user, String clientName) {
        this.oAuth2User = user;
        this.clientName = clientName;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oAuth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oAuth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oAuth2User.getAttribute("name");
    }

    public String getFullName(){
        return this.fullName != null ? fullName : getName();
    }

    public String getEmail(){
        return oAuth2User.getAttribute("email");
    }

    public String getClientName() {
        return clientName;
    }

    public void setFullName(String fullName){
        this.fullName = fullName;
    }
}
