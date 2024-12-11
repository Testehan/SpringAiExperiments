package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.constants.PromptConstants;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.SupportedCity;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.model.auth.UserProfile;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.EmailService;
import com.testehan.springai.immobiliare.service.UserSseService;
import com.testehan.springai.immobiliare.util.ListingUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.thymeleaf.util.StringUtils;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class MainController {

	private final ApartmentService apartmentService;
	private final ConversationSession conversationSession;
	private final UserSseService userSseService;
	private final EmailService emailService;

	public MainController(ApartmentService apartmentService, ConversationSession conversationSession, UserSseService userSseService, EmailService emailService) {
		this.apartmentService = apartmentService;
		this.conversationSession = conversationSession;
		this.userSseService = userSseService;
		this.emailService = emailService;
	}

	@GetMapping("/")
	public String index(Model model) {
		return "index";
	}

	@GetMapping("/chat")
	public String chat(Model model, HttpSession session) {
		var user = conversationSession.getImmobiliareUser();
		var sessionId = session.getId();
		if (StringUtils.isEmpty(user.getPropertyType())) {
			model.addAttribute("initialMessage", PromptConstants.M01_INITIAL_MESSAGE);
		} else if (StringUtils.isEmpty(user.getCity()) || 0 == SupportedCity.valueOf(user.getCity()).compareTo(SupportedCity.UNSUPPORTED)){
			model.addAttribute("initialMessage", PromptConstants.M02_CITY);
		} else {
			model.addAttribute("initialMessage", String.format(PromptConstants.M03_DETAILS,user.getPropertyType(), SupportedCity.valueOf(user.getCity()).getName()));
		}
		var userSseId = userSseService.addUserSseId(sessionId);
		model.addAttribute("sseId", userSseId);

		return "chat";
	}

	@GetMapping("/favourites")
	public String favourites(Model model) {
		var user = conversationSession.getImmobiliareUser();
		List<Apartment> apartments = new ArrayList<>();
		for (String apartmentId : user.getFavouriteProperties()){
			if (!StringUtils.isEmpty(apartmentId)) {
				apartmentService.findApartmentById(apartmentId).ifPresent(apartment -> apartments.add(apartment));
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
		model.addAttribute("listCities",SupportedCity.getSupportedCities());
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
			var apartmentOptional = apartmentService.findApartmentById(apartmentId);
			if (!apartmentOptional.isEmpty()) {
				var apartment = apartmentOptional.get();
				model.addAttribute("apartment", apartment);
				model.addAttribute("numberOfExistingImages", apartment.getImages().size());
				model.addAttribute("buttonMessage", "Update Apartment");
			}
		}

		model.addAttribute("listCities",SupportedCity.getSupportedCities());
		model.addAttribute("listPropertyTypes",List.of("rent", "sale"));

		List<Apartment> listOfProperties = getListOfProperties(user);;
		model.addAttribute("listOfProperties", listOfProperties);

		return "add";
	}

	@GetMapping("/view/{apartmentId}")
	public String view(@PathVariable(value = "apartmentId") String apartmentId, Model model) {
		var apartmentOptional = apartmentService.findApartmentById(apartmentId);
		if (!apartmentOptional.isEmpty()) {
			var apartment = apartmentOptional.get();
			var user = conversationSession.getImmobiliareUser();
			var isFavourite = ListingUtil.isApartmentAlreadyFavourite(apartmentId, user);

			model.addAttribute("apartment", apartment);
			model.addAttribute("favouriteButtonStartMessage", ListingUtil.getFavouritesText(isFavourite));
			model.addAttribute("pageName", "view");

			return "view";
		} else {
			model.addAttribute("errorMessage", "It seems the page you're looking for doesn't exist or you don't have access.");
			return "error-404";
		}

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

		UserProfile userProfile = new UserProfile(user.getEmail(), user.getName(), SupportedCity.getByName(user.getCity()).getName(),
				user.getPropertyType(),user.getLastPropertyDescription(),
				user.getSearchesAvailable(), user.getMaxNumberOfListedProperties());

		model.addAttribute("user", userProfile);

		model.addAttribute("listCities", SupportedCity.getSupportedCities());
		model.addAttribute("listPropertyTypes",List.of("rent", "sale"));

		return "profile";
	}

	@GetMapping("/contact")
	public String contact(Model model, Principal principal) {
		if (principal != null) {
			var user = conversationSession.getImmobiliareUser();
			UserProfile userProfile = new UserProfile(user.getEmail(), user.getName(), SupportedCity.valueOf(user.getCity()).getName(),
					user.getPropertyType(),user.getLastPropertyDescription(),
					user.getSearchesAvailable(), user.getMaxNumberOfListedProperties());

			model.addAttribute("user", userProfile);
		} else {
			model.addAttribute("user", null);
		}
		return "contact";
	}

	@GetMapping("/confirmation")
	public String confirmation(Model model, HttpSession session) {
		model.addAttribute("confirmationMessage", session.getAttribute("confirmationMessage"));
		return "confirmation";
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