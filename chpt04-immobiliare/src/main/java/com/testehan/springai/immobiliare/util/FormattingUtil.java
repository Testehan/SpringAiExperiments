package com.testehan.springai.immobiliare.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class FormattingUtil {

    public String getFormattedDateCustom(LocalDateTime date) {
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return date.format(customFormatter);
    }

}
