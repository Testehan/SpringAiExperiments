package com.testehan.springai.ollama.chpt05.service;

import com.testehan.springai.ollama.chpt05.config.Constants;
import com.testehan.springai.ollama.chpt05.model.ExtractedPropertyInformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import java.util.Map;

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
        log.info("Response from LLM {}",jsonOutput);
        return jsonOutput;
    }
}
