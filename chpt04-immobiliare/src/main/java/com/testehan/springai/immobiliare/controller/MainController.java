package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.configuration.BeanConfig;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.SupportedCity;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.model.auth.UserProfile;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.UserSseService;
import com.testehan.springai.immobiliare.util.ListingUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
public class MainController {

	@Value("${app.url}")
	private String appUrl;

	private final ApartmentService apartmentService;
	private final ConversationSession conversationSession;
	private final UserSseService userSseService;
	private final MessageSource messageSource;
	private final BeanConfig beanConfig;

	public MainController(ApartmentService apartmentService, ConversationSession conversationSession,
						  UserSseService userSseService, MessageSource messageSource,
						  BeanConfig beanConfig) {
		this.apartmentService = apartmentService;
		this.conversationSession = conversationSession;
		this.userSseService = userSseService;
		this.messageSource = messageSource;
		this.beanConfig = beanConfig;
	}

	@GetMapping("/")
	public String index(Model model) {
		return "index";
	}

	@GetMapping("/chat")
	public String chat(Model model, HttpSession session, Locale locale) {
		var user = conversationSession.getImmobiliareUser();
		var sessionId = session.getId();
		if (StringUtils.isEmpty(user.getPropertyType())) {
			model.addAttribute("initialMessage", messageSource.getMessage("M01_INITIAL_MESSAGE", null, locale));
		} else if (StringUtils.isEmpty(user.getCity()) || 0 == SupportedCity.valueOf(user.getCity()).compareTo(SupportedCity.UNSUPPORTED)){
			model.addAttribute("initialMessage", messageSource.getMessage("M02_CITY", null, locale));
		} else {
			var city = SupportedCity.valueOf(user.getCity()).getName();
			var propertyType =  messageSource.getMessage(user.getPropertyType(), null, locale);
			model.addAttribute("initialMessage",
					messageSource.getMessage("M03_DETAILS",  new Object[]{propertyType, city}, locale) +
					messageSource.getMessage("M03_DETAILS_PART_2",  null,locale));
		}
		var userSseId = userSseService.addUserSseId(sessionId);
		model.addAttribute("sseId", userSseId);
		model.addAttribute("googleMapsApiKey", beanConfig.getGoogleMapsApiKey());
		model.addAttribute("saveFavouritesTranslated", messageSource.getMessage("listing.favourites",null,locale));
		model.addAttribute("M01_INITIAL_MESSAGE", messageSource.getMessage("M01_INITIAL_MESSAGE",null,locale));
		model.addAttribute("M02_CITY", messageSource.getMessage("M02_CITY",null,locale));
		model.addAttribute("M03_DETAILS_PART_2", messageSource.getMessage("M03_DETAILS_PART_2",  null,locale));
		model.addAttribute("M04_APARTMENTS_FOUND_START", messageSource.getMessage("M04_APARTMENTS_FOUND_START",  null,locale));
		model.addAttribute("listingShareError", messageSource.getMessage("listing.share.error",  null,locale));
		model.addAttribute("appUrl", appUrl);

		return "chat";
	}

	@GetMapping("/favourites")
	public String favourites(Model model, Locale locale) {
		var user = conversationSession.getImmobiliareUser();
		List<Apartment> apartments = new ArrayList<>();
		for (String apartmentId : user.getFavouriteProperties()){
			if (!StringUtils.isEmpty(apartmentId)) {
				apartmentService.findApartmentById(apartmentId).ifPresent(apartment -> apartments.add(apartment));
			}
		}

		var sortedApartments = apartments.stream().sorted(Comparator.comparing(Apartment::isActive).reversed())  // Inactive (false) at the end
				.collect(Collectors.toList());

		model.addAttribute("apartments", sortedApartments);
		model.addAttribute("googleMapsApiKey", beanConfig.getGoogleMapsApiKey());
		model.addAttribute("saveFavouritesTranslated", messageSource.getMessage("listing.favourites",null,locale));
		model.addAttribute("listingShareError", messageSource.getMessage("listing.share.error",  null,locale));
		model.addAttribute("appUrl", appUrl);

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
	public String add(Model model, Locale locale) {

		var apartment = new Apartment();
		model.addAttribute("listCities",SupportedCity.getSupportedCities());
		model.addAttribute("listPropertyTypes",List.of("rent", "sale"));
		model.addAttribute("apartment", apartment);
		model.addAttribute("numberOfExistingImages", 0);
		var buttonMessage = messageSource.getMessage("add.button.add", null, locale);
		var deleteButtonMessage = messageSource.getMessage("add.a.deleteimage", null, locale);
		var imageNoLabel = messageSource.getMessage("add.label.imagenumber", null, locale);
		model.addAttribute("buttonMessage", buttonMessage);
		model.addAttribute("deleteButtonMessage", deleteButtonMessage);
		model.addAttribute("imageNoLabel", imageNoLabel);
		model.addAttribute("appUrl", appUrl);

		var user = conversationSession.getImmobiliareUser();
		List<Apartment> listOfProperties = getListOfProperties(user);

		model.addAttribute("listOfProperties", listOfProperties);

		return "add";
	}

	@GetMapping("/edit/{apartmentId}")
	public String edit(@PathVariable(value = "apartmentId") String apartmentId, Model model, Locale locale) {

		var user = conversationSession.getImmobiliareUser();
		model.addAttribute("apartment", new Apartment());
		model.addAttribute("numberOfExistingImages", 0);
		var buttonMessage = messageSource.getMessage("add.button.add", null, locale);
		var deleteButtonMessage = messageSource.getMessage("add.a.deleteimage", null, locale);
		var imageNoLabel = messageSource.getMessage("add.label.imagenumber", null, locale);
		model.addAttribute("buttonMessage", buttonMessage);
		model.addAttribute("deleteButtonMessage", deleteButtonMessage);
		model.addAttribute("imageNoLabel", imageNoLabel);
		model.addAttribute("appUrl", appUrl);

		if (user.getListedProperties().contains(apartmentId) || user.isAdmin()) {
			var apartmentOptional = apartmentService.findApartmentById(apartmentId);
			if (!apartmentOptional.isEmpty()) {
				var apartment = apartmentOptional.get();
				model.addAttribute("apartment", apartment);
				model.addAttribute("numberOfExistingImages", apartment.getImages().size());
				buttonMessage = messageSource.getMessage("add.button.update", null, locale);
				model.addAttribute("buttonMessage", buttonMessage);
			}
		}

		model.addAttribute("listCities",SupportedCity.getSupportedCities());
		model.addAttribute("listPropertyTypes",List.of("rent", "sale"));

		List<Apartment> listOfProperties = getListOfProperties(user);;
		model.addAttribute("listOfProperties", listOfProperties);

		return "add";
	}

	@GetMapping("/view/{apartmentId}")
	public String view(@PathVariable(value = "apartmentId") String apartmentId, Model model, Locale locale) {
		var apartmentOptional = apartmentService.findApartmentById(apartmentId);
		if (!apartmentOptional.isEmpty()) {
			var apartment = apartmentOptional.get();
			var user = conversationSession.getImmobiliareUser();
			var isFavourite = ListingUtil.isApartmentAlreadyFavourite(apartmentId, user);
			var favouritesText = ListingUtil.getFavouritesText(isFavourite);
			if (favouritesText.equalsIgnoreCase("listing.favourites")){
				favouritesText = messageSource.getMessage("listing.favourites",null,locale);
			}

			model.addAttribute("apartment", apartment);
			model.addAttribute("googleMapsApiKey", beanConfig.getGoogleMapsApiKey());
			model.addAttribute("favouriteButtonStartMessage", favouritesText);
			model.addAttribute("saveFavouritesTranslated", messageSource.getMessage("listing.favourites",null,locale));
			model.addAttribute("listingShareError", messageSource.getMessage("listing.share.error",  null,locale));
			model.addAttribute("pageName", "view");
			model.addAttribute("appUrl", appUrl);

			return "view";
		} else {
			model.addAttribute("errorMessage", "It seems the page you're looking for doesn't exist or you don't have access.");
			return "error-404";
		}

	}

	@GetMapping("/help")
	public String help(Model model) {
		return "help";
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

	@GetMapping("/privacy-policy")
	public String privacy(Locale locale) {
		String localeString = locale.getLanguage();
		if (localeString.equals("ro")) {
			return "privacy-ro";
		} else {
			return "privacy-en";
		}

	}

	@GetMapping("/terms")
	public String terms(Locale locale) {
		String localeString = locale.getLanguage();
		if (localeString.equals("ro")) {
			return "terms-ro";
		} else {
			return "terms-en";
		}

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