package com.testehan.springai.immobiliare.security;


import com.testehan.springai.immobiliare.repository.ImmobiliareUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// service used for the login of customers
@Service
public class CustomerUserDetailsService implements UserDetailsService {

    @Autowired
    private ImmobiliareUserRepository immobiliareUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var customer = immobiliareUserRepository.findUserByEmail(email);
        if (customer.isPresent()) {
            return new CustomerUserDetails(customer.get());
        } else {
            throw new UsernameNotFoundException("Customer with email " + email + " does not exist");
        }
    }
}
