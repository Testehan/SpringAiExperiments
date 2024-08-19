package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.constants.PromptConstants;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {


	@GetMapping("/")
	public String index(Model model) {
		return "index";
	}

	@GetMapping("/chat")
	public String chat(Model model) {
		model.addAttribute("initialMessage", PromptConstants.M01_INITIAL_MESSAGE);
		return "chat";
	}

	@GetMapping("/login")
	public String viewLoginPage() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication==null || authentication instanceof AnonymousAuthenticationToken) {
			return "login";
		} else {
			return "index";	// to homepage
		}
	}
}