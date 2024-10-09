package com.testehan.springai.immobiliare.security;

import com.testehan.springai.immobiliare.model.auth.AuthenticationType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.repository.ImmobiliareUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private ImmobiliareUserRepository immobiliareUserRepository;

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

    public ImmobiliareUser getImmobiliareUserByEmail(String email){
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

        immobiliareUserRepository.save(user);
    }
}
