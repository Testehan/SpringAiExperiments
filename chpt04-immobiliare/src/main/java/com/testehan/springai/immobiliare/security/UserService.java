package com.testehan.springai.immobiliare.security;

import com.testehan.springai.immobiliare.model.auth.AuthenticationType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.repository.ImmobiliareUserRepository;
import com.testehan.springai.immobiliare.service.EmailService;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final EmailService emailService;
    private final ImmobiliareUserRepository immobiliareUserRepository;
    private final LocaleUtils localeUtils;

    public UserService( HttpServletRequest request, HttpServletResponse response,
                       EmailService emailService, ImmobiliareUserRepository immobiliareUserRepository, LocaleUtils localeUtils) {
        this.request = request;
        this.response = response;
        this.emailService = emailService;
        this.immobiliareUserRepository = immobiliareUserRepository;
        this.localeUtils = localeUtils;
    }

    public boolean isEmailUnique(String email) {
        var user = immobiliareUserRepository.findUserByEmail(email);
        return user == null;
    }

    public void updateAuthenticationType(ImmobiliareUser user, AuthenticationType authenticationType){
        if (!user.getAuthenticationType().equals(authenticationType)) {
            immobiliareUserRepository.updateAuthenticationType(user.getId(), authenticationType);
        }
    }

    public void updateUser(ImmobiliareUser user){
        immobiliareUserRepository.update(user);
    }

    public void deleteUser(ImmobiliareUser user){
        immobiliareUserRepository.deleteById(user.getId());
        logoutUser();
    }

    public Optional<ImmobiliareUser> getImmobiliareUserByEmail(String email){
        return immobiliareUserRepository.findUserByEmail(email);
    }

    public void resetSearchesAvailable(){
        immobiliareUserRepository.resetSearchesAvailable();
    }


    public void addNewCustomerAfterOAuth2Login(String name, String email, AuthenticationType authenticationType) {
        var user = new ImmobiliareUser();

        user.setName(name);
        user.setEmail(email);
        user.setAuthenticationType(authenticationType);
        user.setPassword("");
        user.setIsAdmin("false");

        immobiliareUserRepository.save(user);
        emailService.sendWelcomeEmail(email, name, localeUtils.getCurrentLocale());
    }

    private void logoutUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
    }
}
