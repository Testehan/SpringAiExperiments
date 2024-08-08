package com.testehan.springai.immobiliare.security;

import com.testehan.springai.immobiliare.model.auth.Customer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

// A Customer as defined by us, but with extra information that is integrated into the spring security.
// This is the "principal" object used in the html files
public class CustomerUserDetails implements UserDetails {

    private Customer customer;

    public CustomerUserDetails(Customer customer) {
        this.customer = customer;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;    // because for customers we don't have authorities
    }

    @Override
    public String getPassword() {
        return customer.password();
    }

    @Override
    public String getUsername() {
        return customer.email();
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

    public Customer getCustomer() {
        return customer;
    }
}
