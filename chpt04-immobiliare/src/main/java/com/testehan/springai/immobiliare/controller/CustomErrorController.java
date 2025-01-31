package com.testehan.springai.immobiliare.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if(statusCode == HttpStatus.NOT_FOUND.value() || statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("errorMessage", "It seems the page you're looking for doesn't exist or you don't have access.");
                return "error-404";
            }
        }

        model.addAttribute("errorMessage", "Something went wrong. You can contact me and i will try to investigate the issue ASAP.");
        return "error";
    }
}
