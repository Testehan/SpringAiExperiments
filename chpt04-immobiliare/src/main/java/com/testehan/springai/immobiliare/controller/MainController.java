package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.constants.PromptConstants;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Controller
public class MainController {

	private final ApartmentService apartmentService;
	private final UserService userService;

	public MainController(ApartmentService apartmentService, UserService userService) {
		this.apartmentService = apartmentService;
		this.userService = userService;
	}

	@GetMapping("/")
	public String index(Model model) {
		return "index";
	}

	@GetMapping("/chat")
	public String chat(Model model) {
		model.addAttribute("initialMessage", PromptConstants.M01_INITIAL_MESSAGE);
		return "chat";
	}

	@GetMapping("/favourites")
	public String favourites(Model model, Authentication authentication) {
		String userEmail = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("email");

		var user = userService.getImmobiliareUserByEmail(userEmail);
		List<Apartment> apartments = new ArrayList<>();
		for (String apartmentId : user.getFavourites()){
			if (!StringUtils.isEmpty(apartmentId)) {
				apartments.add(apartmentService.findApartmentById(apartmentId));
			}
		}

		model.addAttribute("apartments", apartments);
		return "favourites";
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

	@GetMapping("/add")
	public String add(Model model) {
		return "index";
	}

	@GetMapping("/help")
	public String help(Model model) {
		return "index";
	}

	@GetMapping("/blog")
	public String blog(Model model) {
		return "index";
	}

	@GetMapping("/contact")
	public String contact(Model model) {
		return "index";
	}
}