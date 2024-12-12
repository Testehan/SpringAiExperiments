package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApiService;
import com.testehan.springai.immobiliare.service.OpenAiService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import static com.testehan.springai.immobiliare.constants.PromptConstants.M00_NO_SEARCH_QUERIES_AVAILABLE;
import static com.testehan.springai.immobiliare.constants.PromptConstants.M00_SEARCH_QUERIES_AVAILABLE;

@Controller
@CrossOrigin
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    private final ApiService apiService;
    private final OpenAiService openAiService;
    private final ConversationSession conversationSession;
    private final UserService userService;

    public ChatController(ApiService apiService, OpenAiService openAiService, ConversationSession conversationSession,
                          UserService userService) {
        this.apiService = apiService;
        this.openAiService = openAiService;
        this.conversationSession = conversationSession;
        this.userService = userService;
    }

    @HxRequest
    @PostMapping("/respond")
    public HtmxResponse respond(@RequestParam String message, Model model, HttpSession session) {

        var user  = conversationSession.getImmobiliareUser();
        var searchQueriesAvailable = user.getSearchesAvailable();
        if (searchQueriesAvailable > 0) {
            var response = apiService.getChatResponse(message, session);

            user  = conversationSession.getImmobiliareUser();   // getting the user again, because it might have been updated during the chat call
            user.setSearchesAvailable(searchQueriesAvailable - 1);
            userService.updateUser(user);

            if (searchQueriesAvailable <= 5){
                var queriesAvailableMessage = String.format(M00_SEARCH_QUERIES_AVAILABLE, searchQueriesAvailable);
                model.addAttribute("queriesAvailableMessage", queriesAvailableMessage);
            }
            model.addAttribute("response", response.message());

        } else {
            model.addAttribute("response", M00_NO_SEARCH_QUERIES_AVAILABLE);
        }

    // TODO
//        model.addAttribute("response", "this is a test for noqw");
        return HtmxResponse.builder()
                .view("response :: responseFragment")
                .build();
    }

    // Add a user message to model but don't ask to respond to it. This enables us to update the UI quickly.
    @HxRequest
    @PostMapping("/message")
    public HtmxResponse addUserMessage(@RequestParam(required = false) String message,
                                       @RequestParam(required = false) MultipartFile audioFile,
                                       Model model) {
        if (message.isEmpty()) {
            LOGGER.info("Audio file sent from user for transcription ");
            message = openAiService.transcribeAudioMessage(audioFile.getResource());
        }
        model.addAttribute("message",message);
        return HtmxResponse.builder()
                .view("response :: responseFragment")
                .build();
    }

}
