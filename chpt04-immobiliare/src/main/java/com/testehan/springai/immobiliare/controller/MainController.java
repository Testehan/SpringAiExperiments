package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.constants.PromptConstants;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.model.auth.UserProfile;
import com.testehan.springai.immobiliare.service.ApartmentService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.thymeleaf.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Controller
public class MainController {

	private final ApartmentService apartmentService;
	private final ConversationSession conversationSession;

	public MainController(ApartmentService apartmentService, ConversationSession conversationSession) {
		this.apartmentService = apartmentService;
		this.conversationSession = conversationSession;
	}

	@GetMapping("/")
	public String index(Model model) {
		return "index";
	}

	@GetMapping("/chat")
	public String chat(Model model) {
		var user = conversationSession.getImmobiliareUser();
		if (StringUtils.isEmpty(user.getPropertyType())) {
			model.addAttribute("initialMessage", PromptConstants.M01_INITIAL_MESSAGE);
		} else if (StringUtils.isEmpty(user.getCity())){
			model.addAttribute("initialMessage", PromptConstants.M02_CITY);
		} else {
			model.addAttribute("initialMessage", String.format(PromptConstants.M03_DETAILS,user.getPropertyType(), user.getCity()));
		}
		return "chat";
	}

	@GetMapping("/favourites")
	public String favourites(Model model) {
		var user = conversationSession.getImmobiliareUser();
		List<Apartment> apartments = new ArrayList<>();
		for (String apartmentId : user.getFavouriteProperties()){
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

		var apartment = new Apartment();
		// todo for now the list of cities available for posting properties is hardcoded here
		model.addAttribute("listCities",List.of("Cluj-Napoca", "Bucharest"));
		model.addAttribute("listPropertyTypes",List.of("rent", "sale"));
		model.addAttribute("apartment", apartment);
		model.addAttribute("numberOfExistingImages", 0);
		model.addAttribute("buttonMessage", "Add Apartment");

		var user = conversationSession.getImmobiliareUser();
		List<Apartment> listOfProperties = getListOfProperties(user);

		model.addAttribute("listOfProperties", listOfProperties);

		return "add";
	}

	@GetMapping("/edit/{apartmentId}")
	public String edit(@PathVariable(value = "apartmentId") String apartmentId, Model model) {

		var user = conversationSession.getImmobiliareUser();
		model.addAttribute("apartment", new Apartment());
		model.addAttribute("numberOfExistingImages", 0);
		model.addAttribute("buttonMessage", "Add Apartment");

		if (user.getListedProperties().contains(apartmentId) || user.isAdmin()) {
			var apartment = apartmentService.findApartmentById(apartmentId);
			if (apartment != null) {
				model.addAttribute("apartment", apartment);
				model.addAttribute("numberOfExistingImages", apartment.getImages().size());
				model.addAttribute("buttonMessage", "Edit Apartment");
			}
		}

		model.addAttribute("listCities",List.of("Cluj-Napoca", "Bucharest"));
		model.addAttribute("listPropertyTypes",List.of("rent", "sale"));

		List<Apartment> listOfProperties = getListOfProperties(user);;
		model.addAttribute("listOfProperties", listOfProperties);

		return "add";
	}

	@GetMapping("/help")
	public String help(Model model) {
		return "index";
	}

	@GetMapping("/blog")
	public String blog(Model model) {
		return "index";
	}

	@GetMapping("/profile")
	public String profile(Model model) {
		var user = conversationSession.getImmobiliareUser();
		UserProfile userProfile = new UserProfile(user.getEmail(),user.getCity(),user.getPropertyType(),user.getLastPropertyDescription());

		model.addAttribute("user", userProfile);
		// todo for now the list of cities available for posting properties is hardcoded here
		model.addAttribute("listCities",List.of("Cluj-Napoca", "Bucharest"));
		model.addAttribute("listPropertyTypes",List.of("rent", "sale"));
		return "profile";
	}

	@GetMapping("/contact")
	public String contact(Model model) {
		return "index";
	}

	private List<Apartment> getListOfProperties(ImmobiliareUser user) {
		List<Apartment> listOfProperties;
		if (user.isAdmin()){
			listOfProperties = apartmentService.findAll();
		} else{
			listOfProperties = apartmentService.findApartmentsByIds(user.getListedProperties());
		}
		return listOfProperties;
	}
}