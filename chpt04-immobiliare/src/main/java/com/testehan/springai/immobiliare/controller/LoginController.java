package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.HashSet;
import java.util.Set;

@RestController
public class LoginController {

    private final SpringTemplateEngine templateEngine;

    public LoginController(SpringTemplateEngine templateEngine){
        this.templateEngine = templateEngine;
    }

    @GetMapping("/login-modal")
    public String loginModal(HttpServletRequest req, HttpServletResponse resp) {
        var webApplication = JakartaServletWebApplication.buildApplication(req.getServletContext());
        final IWebExchange webExchange = webApplication.buildExchange(req, resp);
        WebContext context = new WebContext(webExchange);
		Set<String> selectors = new HashSet<>();
		selectors.add("loginModal");
        context.setLocale(LocaleUtils.getCurrentLocale());

		var data = templateEngine.process("fragments",selectors, context).
				replaceAll("[\\n\\r]+", "");
		return data;
    }
}
