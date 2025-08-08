package com.testehan.springai.ollama.chpt05.service;

import com.testehan.springai.ollama.chpt05.config.Constants;
import com.testehan.springai.ollama.chpt05.model.ExtractedPropertyInformation;
import com.testehan.springai.ollama.chpt05.model.ListingValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class OllamaService {
    private final OllamaChatModel ollamaChatModel;

    public OllamaService(OllamaChatModel ollamaChatModel) {
        this.ollamaChatModel = ollamaChatModel;
    }

    public String getResponseFromLlm(String input) {
        log.info("Received message {}",input);
        var response = ollamaChatModel.call(input);

        log.info("Response from deepseek {}",response);
        return response;
    }

    public String getFormatListing(String input) {
        log.info("Received message {}",input);


        String jsonOutput = getListingJsonString(input);
        log.info("Response from LLM {}",jsonOutput);
        ListingValidationResult validation = getListingValidationResultJsonString(input, jsonOutput);

        int retries = 0;
        while (!validation.isConsistent() && retries < 1) {
            log.warn("correction attempt {} ", retries+1);
            jsonOutput = correctListingJson(input, jsonOutput, validation);
//            validation = getListingValidationResultJsonString(input, jsonOutput);
            retries++;
        }

        if (!validation.isConsistent()) {
            log.warn("------- Nu s-a putut corecta JSON-ul după 1 încercări :(  --> {}" , jsonOutput);
        }

        return jsonOutput;
    }

    private String getListingJsonString(String input) {
        BeanOutputConverter<ExtractedPropertyInformation> outputConverter = new BeanOutputConverter<>(ExtractedPropertyInformation.class);

        PromptTemplate promptTemplate = new PromptTemplate(Constants.PROMPT_FORMAT_LISTING);
        Prompt populatedPrompt = promptTemplate.create(Map.of(
                "rawText", input,
                "format", outputConverter.getFormat()
        ));

        // 1. Create OllamaOptions and set the format to "json".
        // The builder pattern is common here.
        OllamaOptions options = OllamaOptions.builder()
//                .model("your-ollama-model-name") // e.g. "llama3"
                .format("json")
                .temperature(0.1D) // <-- ADD THIS LINE. Use a low value (0.0 to 0.2 is good)
                .build();

        // 2. Create a Prompt object containing your text and the options.
        Prompt prompt = new Prompt(populatedPrompt.getInstructions(), options);

        var response = ollamaChatModel.call(prompt);

        String jsonOutput = response.getResult().getOutput().getText();
        return jsonOutput;
    }

    private ListingValidationResult getListingValidationResultJsonString(String rawInput, String jsonListing) {
        BeanOutputConverter<ListingValidationResult> outputConverter = new BeanOutputConverter<>(ListingValidationResult.class);

        PromptTemplate promptTemplate = new PromptTemplate(Constants.PROMPT_VERIFY_LISTING);
        Prompt populatedPrompt = promptTemplate.create(Map.of(
                "rawText", rawInput,
                "jsonListing", jsonListing,
                "format", outputConverter.getFormat()
        ));

        // 1. Create OllamaOptions and set the format to "json".
        // The builder pattern is common here.
        OllamaOptions options = OllamaOptions.builder()
//                .model("your-ollama-model-name") // e.g. "llama3"
                .format("json")
                .temperature(0.1D) // <-- ADD THIS LINE. Use a low value (0.0 to 0.2 is good)
                .build();

        // 2. Create a Prompt object containing your text and the options.
        Prompt prompt = new Prompt(populatedPrompt.getInstructions(), options);

        var response = ollamaChatModel.call(prompt);

        String jsonOutput = response.getResult().getOutput().getText();
        return outputConverter.convert(jsonOutput);
    }

    private String correctListingJson(String rawInput, String jsonListing, ListingValidationResult validationResult) {
        BeanOutputConverter<ExtractedPropertyInformation> outputConverter = new BeanOutputConverter<>(ExtractedPropertyInformation.class);

        StringBuilder feedback = new StringBuilder("Corectează următoarele probleme:\n");
        if (!Objects.isNull(validationResult.getIssues()) && validationResult.getIssues().size()>0) {
            for (ListingValidationResult.Issue issue : validationResult.getIssues()) {
                feedback.append(String.format("- Câmpul '%s' este %s: %s\n",
                        issue.getField(), issue.getType(), issue.getDescription()));
            }

            PromptTemplate promptTemplate = new PromptTemplate(Constants.PROMPT_CORRECT_LISTING);
            Prompt populatedPrompt = promptTemplate.create(Map.of(
                    "feedback", feedback,
                    "rawText", rawInput,
                    "originalJson", jsonListing,
                    "format", outputConverter.getFormat()
            ));

            // 1. Create OllamaOptions and set the format to "json".
            // The builder pattern is common here.
            OllamaOptions options = OllamaOptions.builder()
//                .model("your-ollama-model-name") // e.g. "llama3"
                    .format("json")
                    .temperature(0.1D) // <-- ADD THIS LINE. Use a low value (0.0 to 0.2 is good)
                    .build();

            // 2. Create a Prompt object containing your text and the options.
            Prompt prompt = new Prompt(populatedPrompt.getInstructions(), options);

            var response = ollamaChatModel.call(prompt);

            String jsonOutput = response.getResult().getOutput().getText();
            return jsonOutput;
        } else {
            return jsonListing;
        }
    }
}
