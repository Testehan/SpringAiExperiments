package com.testehan.springai.immobiliare.util;

import com.testehan.springai.immobiliare.model.auth.AuthenticationType;

public class AuthenticationUtil {

    public static AuthenticationType getAuthenticationType(String clientName){
        if (clientName.equalsIgnoreCase("google")){
            return AuthenticationType.GOOGLE;
        } else if (clientName.equalsIgnoreCase("facebook")){
            return AuthenticationType.FACEBOOK;
        }
        return AuthenticationType.DATABASE;
    }

}
