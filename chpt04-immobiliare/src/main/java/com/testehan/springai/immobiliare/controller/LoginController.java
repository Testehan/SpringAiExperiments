package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
public class LoginController {

    private final SpringTemplateEngine templateEngine;
    private final LocaleUtils localeUtils;
    private final UserService userService;

    public LoginController(SpringTemplateEngine templateEngine, LocaleUtils localeUtils, ConversationSession conversationSession, UserService userService){
        this.templateEngine = templateEngine;
        this.localeUtils = localeUtils;
        this.userService = userService;
    }

    @GetMapping("/login-modal")
    public String loginModal(HttpServletRequest req, HttpServletResponse resp) {
        var webApplication = JakartaServletWebApplication.buildApplication(req.getServletContext());
        final IWebExchange webExchange = webApplication.buildExchange(req, resp);
        WebContext context = new WebContext(webExchange);
		Set<String> selectors = new HashSet<>();
		selectors.add("loginModal");
        context.setLocale(localeUtils.getCurrentLocale());

		var data = templateEngine.process("fragments",selectors, context).
				replaceAll("[\\n\\r]+", "");
		return data;
    }

    @PostMapping("/accept-gdpr")
    public ResponseEntity<?> acceptGdprConsent(@RequestBody Map<String, Object> consentData) {
        Boolean consent = (Boolean) consentData.get("consent");
        String timestamp = (String) consentData.get("timestamp");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser")))
        {
            var user = getImmobiliareUser();
            user.setGdprConsent(consent);
            user.setGdprTimestamp(timestamp);
            userService.updateUser(user);
            return ResponseEntity.ok("Consent stored successfully");
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated.");

    }

    private ImmobiliareUser getImmobiliareUser() {
        String userEmail = ((OAuth2AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getAttribute("email");
        var user = userService.getImmobiliareUserByEmail(userEmail);
        return user.get();
    }
}
