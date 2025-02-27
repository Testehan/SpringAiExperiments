package com.testehan.springai.immobiliare.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FormattingUtil {


    public static String getFormattedDateCustom(LocalDateTime date) {
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return date.format(customFormatter);
    }

}
