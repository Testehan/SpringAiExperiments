package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApiService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import static com.testehan.springai.immobiliare.constants.PromptConstants.M00_NO_SEARCH_QUERIES_AVAILABLE;

@Controller
@CrossOrigin
public class ChatController {

    private final ApiService apiService;
    private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    private final ConversationSession conversationSession;
    private final UserService userService;

    public ChatController(ApiService apiService, OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel, ConversationSession conversationSession,
                          UserService userService) {
        this.apiService = apiService;
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
        this.conversationSession = conversationSession;
        this.userService = userService;
    }

    @HxRequest
    @PostMapping("/respond")
    public HtmxResponse respond(@RequestParam String message, Model model) {

       var user  = conversationSession.getImmobiliareUser();
        Integer searchQueriesAvailable = user.getSearchesAvailable();
        if (searchQueriesAvailable>0) {
            ResultsResponse response = apiService.getChatResponse(message);

            user  = conversationSession.getImmobiliareUser();   // TODO getting the user again, because it might have been updated during the chat call
            user.setSearchesAvailable(searchQueriesAvailable - 1);
            userService.updateUser(user);


            if (response.containsApartments()) {
                model.addAttribute("response", response.message());
                model.addAttribute("apartments", response.apartments());

                return HtmxResponse.builder()
                        .view("response :: responseFragmentWithApartments")
                        .build();
            } else {
                model.addAttribute("response", response.message());
            }
        } else {
            model.addAttribute("response", M00_NO_SEARCH_QUERIES_AVAILABLE);
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
        if (StringUtils.isEmpty(message)){
            OpenAiAudioApi.TranscriptResponseFormat responseFormat = OpenAiAudioApi.TranscriptResponseFormat.TEXT;

            OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                    .withLanguage("en")
                    .withTemperature(0f)
                    .withResponseFormat(responseFormat)
                    .build();
            AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile.getResource(), transcriptionOptions);
            AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(transcriptionRequest);
            message = response.getResult().getOutput();
        }
        model.addAttribute("message",message);
        return HtmxResponse.builder()
                .view("response :: responseFragment")
                .build();
    }

}
