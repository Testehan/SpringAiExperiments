package com.testehan.springai.immobiliare.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class LocaleUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocaleUtils.class);

    private final ResourceLoader resourceLoader;

    public LocaleUtils(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    public String getLocalizedPrompt(String promptFileName) {
        var locale =  getCurrentLocale();
        Resource resource = resourceLoader.getResource(getLocalizedPromptFilePath(promptFileName, locale));
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Prompt file {} with locale {} could not be read",promptFileName, locale);
            throw new RuntimeException(e);
        }
    }

    public String getLocalizedPromptFilePath(String promptFileName, Locale locale) {
        String language = locale.getLanguage();
        return String.format("classpath:/prompts/%s/%s.txt", language, promptFileName);
    }
}
