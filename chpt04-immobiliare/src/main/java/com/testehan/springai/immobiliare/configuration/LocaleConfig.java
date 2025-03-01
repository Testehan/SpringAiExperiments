package com.testehan.springai.immobiliare.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

@Configuration
public class LocaleConfig implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocaleConfig.class);

    @Value("${app.default.language}")
    private String defaultLanguage;

    @Bean
    public LocaleResolver localeResolver() {
        FixedLocaleResolver localeResolver = new FixedLocaleResolver();
        Locale defaultLocale = new Locale(defaultLanguage);
        localeResolver.setDefaultLocale(defaultLocale);
        Locale.setDefault(defaultLocale); // Ensures the JVM uses defaultLanguage as the default.
        return localeResolver;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages"); // Base name of your messages
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false); // Prevents fallback to system locale
        return messageSource;
    }

    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("Language is set to {}", defaultLanguage);
    }
}
