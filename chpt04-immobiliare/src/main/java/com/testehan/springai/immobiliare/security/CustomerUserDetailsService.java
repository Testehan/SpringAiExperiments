package com.testehan.springai.immobiliare.security;


import com.testehan.springai.immobiliare.repository.ImmobiliareUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Objects;

// service used for the login of customers
public class CustomerUserDetailsService implements UserDetailsService {

    @Autowired
    private ImmobiliareUserRepository immobiliareUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var customer = immobiliareUserRepository.findUserByEmail(email);
        if (Objects.nonNull(customer)) {
            return new CustomerUserDetails(customer.get());
        } else {
            throw new UsernameNotFoundException("Customer with email " + email + " does not exist");
        }
    }
}
