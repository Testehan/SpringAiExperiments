package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.events.Event;
import com.testehan.springai.immobiliare.events.EventPayload;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentImage;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.ApiService;
import com.testehan.springai.immobiliare.service.UserSseService;
import com.testehan.springai.immobiliare.util.ListingUtil;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApartmentController.class);
    private static final String BEST_RESULTS_IMAGE_PATH = "/images/best.png";
    private static final String MOST_FAVOURITE_IMAGE_PATH = "/images/most-favourite.png";
    private static final String TOP_CONTACTED_IMAGE_PATH = "/images/top-contacted.png";

    private final ApartmentService apartmentService;
    private final ConversationSession conversationSession;
    private final UserService userService;
    private final UserSseService userSseService;
    private final ApiService apiService;
    private final SpringWebFluxTemplateEngine templateEngine;

    public ApartmentController(ApartmentService apartmentService, ConversationSession conversationSession,
                               UserService userService, ApiService apiService,
                               SpringWebFluxTemplateEngine templateEngine, UserSseService userSseService)
    {
        this.apartmentService = apartmentService;
        this.conversationSession = conversationSession;
        this.apiService = apiService;
        this.templateEngine = templateEngine;
        this.userSseService = userSseService;
        this.userService = userService;
    }

    @PostMapping("/save")
    public ResponseEntity<String> saveApartment(Apartment apartment, RedirectAttributes redirectAttributes,
                                                @RequestParam(value="apartmentImages", required = false) MultipartFile[] apartmentImages) throws IOException {

        var user = conversationSession.getImmobiliareUser();
        if ((apartment.getId() != null && apartment.getId().toString() != null && !user.getListedProperties().contains(apartment.getId().toString())) && !user.isAdmin()){ // make sure that only owners can edit the ap
            LOGGER.warn("User {} tried to edit property with id {} that was not owned", user.getEmail(), apartment.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You can't perform this edit!");
        }

        if (user.getMaxNumberOfListedProperties() > 0){
            List<ApartmentImage> processedImages = apartmentService.processImages(apartmentImages);
            apartmentService.saveApartmentAndImages(apartment, processedImages, user);
            LOGGER.info("User {} added/edited a property ", user.getEmail());
            // Return a response to the frontend
            return ResponseEntity.ok("We are processing the information provided. Refresh the page in 1 minute.");

        } else {
            LOGGER.warn("User {} tried to add more properties than allowed", user.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You have reached the maximum number of listed apartments!");
        }
    }

    @GetMapping("/contact/{apartmentId}")
    @HxRequest
    public String contact(@PathVariable(value = "apartmentId") String apartmentId) {
        var apartmentOptional = apartmentService.findApartmentById(apartmentId);
        if (!apartmentOptional.isEmpty()) {
            var listing = apartmentOptional.get();
            listing.setNoOfContact(listing.getNoOfContact()+1);
            apartmentService.saveApartment(listing);
            return "Contact: " + apartmentOptional.get().getContact();
        } else {
            LOGGER.error("No apartment with id {} was found" , apartmentId);
            return "No apartment found!";
        }
    }

    @GetMapping("/suggestions/{suggestionStep}")
    public List<String> suggestions(@PathVariable(value = "suggestionStep") Integer suggestionStep) {
        List<String> suggestions = new ArrayList<String>();
        switch (suggestionStep) {
            case 1:
                suggestions = getStep1Suggestions();
                break;
            case 2:
                suggestions = getStep2Suggestions();
                break;
            case 3:
                suggestions = getStep3Suggestions();
                break;
            default:
                return suggestions;
        }
        return suggestions;
    }

    private List<String> getStep1Suggestions() {
        var suggestions = new ArrayList<String>();
        suggestions.add("Rent");
        suggestions.add("Buy");

        return suggestions;
    }

    private List<String> getStep2Suggestions() {
        var suggestions = new ArrayList<String>();
        suggestions.add("Cluj-Napoca");
        suggestions.add("Bucharest");

        return suggestions;
    }

    private List<String> getStep3Suggestions() {
        var suggestions = new ArrayList<String>();
        var user = conversationSession.getImmobiliareUser();
        // TODO this is with low priority but i would add the top 10 searches of the users in the DB
        // and randomly add some of them to the suggestions list that is returned
        suggestions.add(user.getLastPropertyDescription());
        suggestions.add("Pet friendly studio close to city center or universities");
        suggestions.add("3 room apartment for a family with a price below 700 euro ");
        return suggestions;
    }

    @PostMapping("/favourite/{apartmentId}")
    @HxRequest
    public void favourite(@PathVariable(value = "apartmentId") String apartmentId) {
        var user = conversationSession.getImmobiliareUser();
        var apartmentOptional = apartmentService.findApartmentById(apartmentId);
        if (!apartmentOptional.isEmpty()) {
            var listing = apartmentOptional.get();
            if (!user.getFavouriteProperties().contains(apartmentId)) {
                listing.setNoOfFavourite(listing.getNoOfFavourite() + 1);
                user.getFavouriteProperties().add(apartmentId);
            } else {
                listing.setNoOfFavourite(listing.getNoOfFavourite() - 1);
                user.getFavouriteProperties().remove(apartmentId);
            }

            apartmentService.saveApartment(listing);
            userService.updateUser(user);
        } else {
            LOGGER.error("No apartment with id {} was found" , apartmentId);
        }
    }

    @GetMapping(value = "/stream/{sseId}", produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> streamServerSideEvents(@PathVariable String sseId, HttpSession httpSession) {
        userSseService.addUserSseId(httpSession.getId());
        return apiService.getServerSideEventsFlux(httpSession)
                .map(event -> renderServerSideEventData(httpSession, event, sseId));

    }

    private ServerSentEvent<String> renderServerSideEventData(HttpSession httpSession, Event event, String sseId){
        if (event.getEventType().equals("apartment")){
            var sseIndex = httpSession.getAttribute("sseIndex");
            int index = Objects.isNull(sseIndex) ? 0 : (int) sseIndex;
            if (index == 0) {
                httpSession.setAttribute("sseIndex", 1);
            } else {
                httpSession.setAttribute("sseIndex", index + 1);
            }
            return getApartmentServerSentEvent(event.getPayload(),index ,sseId);
        } else {
            httpSession.setAttribute("sseIndex", 0);
            return getResponseServerSideEvent(event.getPayload(),sseId);
        }
    }

    private ServerSentEvent<String> getResponseServerSideEvent(EventPayload eventPayload, String sseId) {
        Context context = new Context();
        Set<String> selectors = new HashSet<>();
        selectors.add("responseFragmentWithApartments");
        context.setVariable("response", eventPayload.getPayload());

        var data = templateEngine.process("response",selectors, context).
                replaceAll("[\\n\\r]+", "");    // because we don't want our result to contain new lines

        return createSSE(data,"response",sseId);
    }

    private ServerSentEvent<String> getApartmentServerSentEvent(EventPayload eventPayload,int index, String sseId) {
        Context context = new Context();
        Set<String> selectors = new HashSet<>();
        var apartment = ((Map<String, Object>)eventPayload.getPayload()).get("apartment");
        var isFavourite = (boolean)((Map<String, Object>)eventPayload.getPayload()).get("isFavourite");

        selectors.add("apartment");
        context.setVariable("apartment", apartment);
        context.setVariable("favouriteButtonStartMessage", ListingUtil.getFavouritesText(isFavourite));
        context.setVariable("pageName", "chat");
        context.setVariable("index", index);
        context.setVariable("bestResultsImagePath", BEST_RESULTS_IMAGE_PATH);
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
