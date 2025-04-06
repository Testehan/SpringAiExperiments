package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApiService;
import com.testehan.springai.immobiliare.service.OpenAiService;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@CrossOrigin
public class ChatViewController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatViewController.class);

    private final ApiService apiService;
    private final OpenAiService openAiService;
    private final ConversationSession conversationSession;
    private final UserService userService;
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public ChatViewController(ApiService apiService, OpenAiService openAiService, ConversationSession conversationSession,
                              UserService userService, MessageSource messageSource, LocaleUtils localeUtils) {
        this.apiService = apiService;
        this.openAiService = openAiService;
        this.conversationSession = conversationSession;
        this.userService = userService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @HxRequest
    @PostMapping("/respond")
    public HtmxResponse respond(@RequestParam String message, Model model, HttpSession session) {

        var user  = conversationSession.getImmobiliareUser().get();
        var searchQueriesAvailable = user.getSearchesAvailable();
        if (searchQueriesAvailable > 0) {
            var response = apiService.getChatResponse(message, session);

            user  = conversationSession.getImmobiliareUser().get();   // getting the user again, because it might have been updated during the chat call
            user.setSearchesAvailable(searchQueriesAvailable - 1);
            userService.updateUser(user);

            if (searchQueriesAvailable <= 5){
                model.addAttribute("queriesAvailableMessage", messageSource.getMessage("M00_SEARCH_QUERIES_AVAILABLE", new Object[]{searchQueriesAvailable}, localeUtils.getCurrentLocale()));
            }
            model.addAttribute("response", response.message());

        } else {
            model.addAttribute("response", messageSource.getMessage("M00_NO_SEARCH_QUERIES_AVAILABLE", null, localeUtils.getCurrentLocale()));
        }

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
            var optionalMessage = openAiService.transcribeAudioMessage(audioFile.getResource());
            if (optionalMessage.isPresent()) {
                message = optionalMessage.get();
            } else {
                message =  messageSource.getMessage("chat.exception", null, localeUtils.getCurrentLocale());
            }
        }
        model.addAttribute("message",message);
        return HtmxResponse.builder()
                .view("response :: responseFragment")
                .build();
    }

}
