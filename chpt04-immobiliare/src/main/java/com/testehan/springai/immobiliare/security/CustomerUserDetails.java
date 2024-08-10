package com.testehan.springai.immobiliare.security;

import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

// A Customer as defined by us, but with extra information that is integrated into the spring security.
// This is the "principal" object used in the html files
public class CustomerUserDetails implements UserDetails {

    private ImmobiliareUser immobiliareUser;

    public CustomerUserDetails(ImmobiliareUser customer) {
        this.immobiliareUser = customer;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;    // because for customers we don't have authorities
    }

    @Override
    public String getPassword() {
        return immobiliareUser.getPassword();
    }

    @Override
    public String getUsername() {
        return immobiliareUser.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public ImmobiliareUser getImmobiliareUser() {
        return immobiliareUser;
    }
}
