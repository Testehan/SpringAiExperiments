package com.testehan.springai.immobiliare.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.testehan.springai.immobiliare.util.LocaleUtils;

@Controller
public class CustomErrorController implements ErrorController {

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public CustomErrorController(MessageSource messageSource, LocaleUtils localeUtils) {
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if(statusCode == HttpStatus.NOT_FOUND.value() || statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("errorMessage", messageSource.getMessage("error.notfound",null, localeUtils.getCurrentLocale()));
                return "error-404";
            }
        }

        model.addAttribute("errorMessage", messageSource.getMessage("error.unknown",null, localeUtils.getCurrentLocale()));
        return "error";
    }
}
