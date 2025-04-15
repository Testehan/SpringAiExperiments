package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.events.Event;
import com.testehan.springai.immobiliare.events.EventPayload;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentImage;
import com.testehan.springai.immobiliare.security.SessionCleanupListener;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.*;
import com.testehan.springai.immobiliare.util.ListingUtil;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.util.StringUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApartmentApiController.class);
    private static final String NEW_IMAGE_PATH = "/images/new.png";
    private static final String BEST_RESULTS_IMAGE_PATH = "/images/best.png";
    private static final String MOST_FAVOURITE_IMAGE_PATH = "/images/most-favourite.png";
    private static final String TOP_CONTACTED_IMAGE_PATH = "/images/top-contacted.png";

    private final ApartmentService apartmentService;
    private final ApartmentCrudService apartmentCrudService;
    private final ListingImageService listingImageService;
    private final ConversationSession conversationSession;
    private final UserService userService;
    private final EmbeddingService embeddingService;
    private final UserSseService userSseService;
    private final SessionCleanupListener sessionCleanupListener;
    private final ApiService apiService;
    private final SpringWebFluxTemplateEngine templateEngine;
    private final MessageSource messageSource;

    private final LocaleUtils localeUtils;
    private final ListingUtil listingUtil;

    public ApartmentApiController(ApartmentService apartmentService, ApartmentCrudService apartmentCrudService, ListingImageService listingImageService, ConversationSession conversationSession,
                                  UserService userService, EmbeddingService embeddingService, ApiService apiService,
                                  SpringWebFluxTemplateEngine templateEngine, UserSseService userSseService,
                                  SessionCleanupListener sessionCleanupListener,
                                  MessageSource messageSource, LocaleUtils localeUtils, ListingUtil listingUtil)
    {
        this.apartmentService = apartmentService;
        this.apartmentCrudService = apartmentCrudService;
        this.listingImageService = listingImageService;
        this.conversationSession = conversationSession;
        this.apiService = apiService;
        this.templateEngine = templateEngine;
        this.userSseService = userSseService;
        this.sessionCleanupListener = sessionCleanupListener;
        this.userService = userService;
        this.embeddingService = embeddingService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
        this.listingUtil = listingUtil;
    }

    @PostMapping("/save")
    public ResponseEntity<String> saveApartment(Apartment apartment,
                                                @RequestParam(value="apartmentImages", required = false) MultipartFile[] apartmentImages) throws IOException {

        var user = conversationSession.getImmobiliareUser().get();

        var isPhoneValid = isPhoneValid(apartment.getContact());
        if (!isPhoneValid){
            LOGGER.warn("User {} tried to add phone number {} that already exists", user.getEmail(), apartment.getContact());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("toastify.add.listing.failure.phone", null,localeUtils.getCurrentLocale()));
        }

        if ((apartment.getId() != null && apartment.getId().toString() != null && !user.getListedProperties().contains(apartment.getId().toString())) && !user.isAdmin()){ // make sure that only owners can edit the ap
            LOGGER.warn("User {} tried to edit property with id {} that was not owned", user.getEmail(), apartment.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("toastify.add.listing.failure.edit", null,localeUtils.getCurrentLocale()));
        }

        if (user.getMaxNumberOfListedProperties() > 0){
            LOGGER.info("User {} trying to add/edit a property called {}", user.getEmail(), apartment.getName());
            List<ApartmentImage> processedImages = listingImageService.processImages(apartmentImages);
            apartmentService.saveApartmentAndImages(apartment, processedImages, user);
            LOGGER.info("User {} added/edited a property ", user.getEmail());
            // Return a response to the frontend
            return ResponseEntity.ok(messageSource.getMessage("toastify.add.listing.success", null,localeUtils.getCurrentLocale()));

        } else {
            LOGGER.warn("User {} tried to add more properties than allowed", user.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("toastify.add.listing.failure.max", null,localeUtils.getCurrentLocale()));
        }
    }

    @PostMapping("/batchsave")
    public ResponseEntity<String> batchSaveApartment(Apartment apartment, @RequestParam(value="apartmentImages", required = false) MultipartFile[] apartmentImages) throws IOException {

        var user = conversationSession.getImmobiliareUser().get();
        if (!user.isAdmin()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only admin can use this endpoint");
        }

        var isPhoneValid = apartmentService.isPhoneValid(apartment.getContact());
        if (!isPhoneValid){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Listing with phone " + apartment.getContact() + " already exists. Saving will be skipped for " + apartment.getName());
        }

        LOGGER.info("Batch save - start");
        List<ApartmentImage> processedImages = listingImageService.processImages(apartmentImages);
        apartmentService.saveApartmentAndImages(apartment, processedImages, user);
        // Return a response to the frontend
        return ResponseEntity.ok(messageSource.getMessage("toastify.add.listing.success", null,localeUtils.getCurrentLocale()));

    }

    @PostMapping("/delete/{listingId}")
    public ResponseEntity<String> deleteListing(@PathVariable(value = "listingId") String listingId) {

        var user = conversationSession.getImmobiliareUser().get();

        if ((listingId != null && !user.getListedProperties().contains(listingId)) && !user.isAdmin()){ // make sure that only owners can delete the ap
            LOGGER.warn("User {} tried to delete property with id {} that was not owned", user.getEmail(), listingId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("toastify.delete.listing.failure.notowner", null,localeUtils.getCurrentLocale()));
        }

        var apartment = apartmentCrudService.findApartmentById(listingId);
        if (apartment.isPresent()) {
            listingImageService.deleteUploadedImages(apartment.get());
            apartmentCrudService.deleteApartmentsByIds(List.of(listingId));
            user.getListedProperties().remove(listingId);
            userService.updateUser(user);
            LOGGER.info("User {} deleted a property ", user.getEmail());
        } else {
            LOGGER.warn("User {} tried to delete property with id {} that does not exist", user.getEmail(), listingId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("toastify.delete.listing.failure.notowner", null,localeUtils.getCurrentLocale()));
        }
        // Return a response to the frontend
        return ResponseEntity.ok(messageSource.getMessage("toastify.delete.listing.success", null,localeUtils.getCurrentLocale()));

    }

    @GetMapping("/contact/{apartmentId}")
    public  ResponseEntity<?> contact(@PathVariable(value = "apartmentId") String apartmentId) {

        var user = conversationSession.getImmobiliareUser();
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You must be logged in");
        }

        var apartmentOptional = apartmentCrudService.findApartmentById(apartmentId);
        if (!apartmentOptional.isEmpty()) {
            var listing = apartmentOptional.get();
            listing.setNoOfContact(listing.getNoOfContact()+1);
            apartmentCrudService.saveApartment(listing);

            return ResponseEntity.ok(listing.getContact()); // Return phone number
        } else {
            LOGGER.error("No apartment with id {} was found" , apartmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No apartment found!");
        }
    }

    @GetMapping("/validate/{phoneNumber}")
    public boolean isPhoneValid(@PathVariable(value = "phoneNumber") String phoneNumber) {
        var user = conversationSession.getImmobiliareUser().get();
        if (user.isAdmin()){
            return true;    // admins can post as many numbers as they want with same phonenumber
        } else {
            return apartmentService.isPhoneValid(phoneNumber);
        }
    }

    @GetMapping("/suggestions/{suggestionStep}")
    public List<String> suggestions(@PathVariable(value = "suggestionStep") Integer suggestionStep, Locale locale) {
        List<String> suggestions = new ArrayList<>();
        switch (suggestionStep) {
            case 1:
                suggestions = getStep1Suggestions(locale);
                break;
            case 2:
                suggestions = getStep2Suggestions();
                break;
            case 3:
                suggestions = getStep3Suggestions(locale);
                break;
            case 4:
                suggestions = getStep4Suggestions();
                break;
            default:
                return suggestions;
        }
        return suggestions;
    }

    private List<String> getStep1Suggestions(Locale locale) {
        var suggestions = new ArrayList<String>();
        suggestions.add(messageSource.getMessage("rent", null,locale));
//        suggestions.add(messageSource.getMessage("buy", null,locale));

        return suggestions;
    }

    private List<String> getStep2Suggestions() {
        var suggestions = new ArrayList<String>();
        suggestions.add("Cluj-Napoca");
//        suggestions.add("Bucharest");

        return suggestions;
    }

    private List<String> getStep3Suggestions(Locale locale){
        var suggestions = new ArrayList<String>();
        suggestions.add("< 550 €");
        suggestions.add("550 € < 800 €");
        suggestions.add("800 € <");

        suggestions.add(messageSource.getMessage("prompt.noBudget",null,locale));

        return suggestions;
    }

    private List<String> getStep4Suggestions()
    {
        var user = conversationSession.getImmobiliareUser().get();
        var lastPropertyDescription = user.getLastPropertyDescription();

        List<String> promptIdeas = new ArrayList<>();
        embeddingService.getTopEmbeddingsByUsageCount().forEach(embedding -> promptIdeas.add(embedding.getText()));

        if (!StringUtils.isEmpty(lastPropertyDescription) && promptIdeas.contains(lastPropertyDescription)){
            promptIdeas.remove(lastPropertyDescription);
        }

        Collections.shuffle(promptIdeas, new Random());
        var suggestions = promptIdeas.subList(0, Math.min(2,promptIdeas.size()));
        if (!StringUtils.isEmpty(lastPropertyDescription)) {
            suggestions.add(lastPropertyDescription);
        }

        return suggestions;
    }

    @GetMapping("/favourite/{apartmentId}")
    public ResponseEntity<?> favourite(@PathVariable(value = "apartmentId") String apartmentId) {
        var userOptional = conversationSession.getImmobiliareUser();

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You must be logged in");
        }

        var apartmentOptional = apartmentCrudService.findApartmentById(apartmentId);
        if (!apartmentOptional.isEmpty()) {
            var listing = apartmentOptional.get();
            var immobiliareUser = userOptional.get();
            if (!immobiliareUser.getFavouriteProperties().contains(apartmentId)) {
                listing.setNoOfFavourite(listing.getNoOfFavourite() + 1);
                immobiliareUser.getFavouriteProperties().add(apartmentId);
            } else {
                listing.setNoOfFavourite(listing.getNoOfFavourite() - 1);
                immobiliareUser.getFavouriteProperties().remove(apartmentId);
            }

            apartmentCrudService.saveApartment(listing);
            userService.updateUser(immobiliareUser);

            return ResponseEntity.ok("ok");
        } else {
            LOGGER.error("No apartment with id {} was found" , apartmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No apartment found!");
        }
    }

    @GetMapping(value = "/stream/{sseId}", produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> streamServerSideEvents(@PathVariable String sseId, HttpSession httpSession, Locale locale) {
        userSseService.addUserSseId(httpSession.getId());

//        Flux<String> sessionDestroyedFlux = sessionCleanupListener.getSessionDestroyedFlux()
//                .filter(session -> session.equals(httpSession.getId()));

        Flux<ServerSentEvent<String>> responsesStream = apiService.getServerSideEventsFlux(httpSession)
                .map(event -> renderServerSideEventData(httpSession, event, sseId, locale));

        // Heartbeat (ping) stream every 25 seconds
        Flux<ServerSentEvent<String>> heartbeatStream = Flux.interval(Duration.ofSeconds(5))
                .map(tick -> ServerSentEvent.<String>builder()
                        .id(sseId)
                        .event("keep-alive")
                        .data("ping")
                        .build());
//                .takeUntilOther(sessionDestroyedFlux);

        return Flux.merge(responsesStream, heartbeatStream);//.doOnCancel(() -> {
//            LOGGER.info("Client disconnected: " + sseId);
//            userSseService.removeUserSseId(httpSession.getId());
//        });

    }

    private ServerSentEvent<String> renderServerSideEventData(HttpSession httpSession, Event event, String sseId, Locale locale){
        if (event.getEventType().equals("apartment")){
            var sseIndex = httpSession.getAttribute("sseIndex");
            int index = Objects.isNull(sseIndex) ? 0 : (int) sseIndex;
            if (index == 0) {
                httpSession.setAttribute("sseIndex", 1);
            } else {
                httpSession.setAttribute("sseIndex", index + 1);
            }
            return getApartmentServerSentEvent(event.getPayload(),index ,sseId, locale);
        } else {
            httpSession.setAttribute("sseIndex", 0);
            return getResponseServerSideEvent(event.getPayload(),sseId, locale);
        }
    }

    private ServerSentEvent<String> getResponseServerSideEvent(EventPayload eventPayload, String sseId, Locale locale) {
        Context context = new Context();
        Set<String> selectors = new HashSet<>();
        selectors.add("responseFragmentWithApartments");
        context.setVariable("response", eventPayload.getPayload());
        context.setLocale(locale);

        var data = templateEngine.process("response",selectors, context).
                replaceAll("[\\n\\r]+", "");    // because we don't want our result to contain new lines

        return createSSE(data,"response",sseId);
    }

    private ServerSentEvent<String> getApartmentServerSentEvent(EventPayload eventPayload,int index, String sseId, Locale locale) {
        Context context = new Context();
        Set<String> selectors = new HashSet<>();
        var apartment = ((Map<String, Object>)eventPayload.getPayload()).get("apartment");
        var isFavourite = (boolean)((Map<String, Object>)eventPayload.getPayload()).get("isFavourite");
        var favouritesText = listingUtil.getFavouritesText(isFavourite);
        if (favouritesText.equalsIgnoreCase("listing.favourites")){
            favouritesText = messageSource.getMessage("listing.favourites",null,locale);
        }

        selectors.add("apartment");
        context.setVariable("apartment", apartment);
        context.setVariable("favouriteButtonStartMessage", favouritesText);
        context.setVariable("pageName", "chat");
        context.setVariable("index", index);
        context.setVariable("bestResultsImagePath", BEST_RESULTS_IMAGE_PATH);

        if (listingUtil.isListingNew((Apartment)apartment)){
            context.setVariable("newImagePath", NEW_IMAGE_PATH);
        }

        if (((Apartment)apartment).isMostContacted()){
            context.setVariable("topContactedImagePath", TOP_CONTACTED_IMAGE_PATH);
        } else {
            context.setVariable("topContactedImagePath", "");
        }
        if (((Apartment)apartment).isMostFavourite()) {
            context.setVariable("mostFavouriteImagePath", MOST_FAVOURITE_IMAGE_PATH);
        } else {
            context.setVariable("mostFavouriteImagePath", "");
        }
        context.setLocale(locale);

        var data = templateEngine.process("fragments",selectors, context).
                replaceAll("[\\n\\r]+", "");    // because we don't want our result to contain new lines

        return createSSE(data,"apartment",sseId);
    }

    private static ServerSentEvent<String> createSSE(String data, String eventType, String sseId) {
        return ServerSentEvent.<String>builder()
                .id(sseId)
                .data(data)
                // Set the event type
                .event(eventType)
                // Set the retry duration
                .retry(Duration.ofMillis(1000))
                // Build the Server-Sent Event
                .build();
    }
}
