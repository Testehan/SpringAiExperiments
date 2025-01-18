package com.testehan.springai.immobiliare.model.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@Getter
@Setter
public class PersistentRememberToken {
        private final String username;
        private final String series;
        private final String tokenValue;
        private final Date date;
}
