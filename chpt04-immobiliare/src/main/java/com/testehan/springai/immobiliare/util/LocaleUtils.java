package com.testehan.springai.immobiliare.util;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public class LocaleUtils {
    public static Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }
}
