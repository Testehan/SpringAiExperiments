package com.testehan.springai.immobiliare;

import jakarta.servlet.http.HttpSession;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.testehan.springai.immobiliare.State.conversations;

@RestController
public class ImmobiliareRagController {

    private final ChatClient chatClient;
    // i think that in the case of an actual usecase, one would need multiple vectore stores,
    // as in a vector store per city-type-type...for ex "london-apartment-rents" or "london-house-sells"
    private final VectorStore immobiliareVectorStore;

    @Value("classpath:/prompts/rag-prompt-template-apartments-sale.txt")
    private Resource ragPromptTemplate;

    public ImmobiliareRagController(ChatClient chatClient, VectorStore immobiliareVectorStore) {
        this.chatClient = chatClient;
        this.immobiliareVectorStore = immobiliareVectorStore;
    }

    @GetMapping("/api/immobiliare")
    public String faq(HttpSession session,
                      @RequestParam(value = "message", defaultValue = "What are some apartments for sale in Marasti?") String message) {

        Message assistantResponse;
        // means the start of a conversation basically
        if (conversations.get(session.getId()) == null){
            List<Document> similarDocuments = immobiliareVectorStore.similaritySearch(SearchRequest.query(message).withTopK(2));
            List<String> contentList = similarDocuments.stream().map(Document::getContent).toList();

            PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
            Map<String, Object> promptParameters = new HashMap<>();
            promptParameters.put("input", message);
            promptParameters.put("documents", String.join("\n", contentList));
            promptParameters.put("userEmail","dante@yahoo.com");
            Prompt prompt = promptTemplate.create(promptParameters);
            Prompt promptWithFunctions =
                    new Prompt(prompt.getContents(),
                            OpenAiChatOptions.builder().withFunction("emailApartmentsFunction").build());

            assistantResponse = chatClient.call(promptWithFunctions).getResult().getOutput();

            List<Message> conversationStart = new ArrayList<>();
            conversationStart.addAll(prompt.getInstructions());
            conversationStart.add(assistantResponse);
            conversations.put(session.getId(),conversationStart);
        } else {    // continuation of a conversation
            Prompt newUserMessage = new Prompt(message);
            conversations.get(session.getId()).addAll(newUserMessage.getInstructions());

            Prompt promptToSend = new Prompt(conversations.get(session.getId()));
            Prompt promptWithFunctions =
                    new Prompt(promptToSend.getContents(),
                            OpenAiChatOptions.builder().withFunction("emailApartmentsFunction").build());
            assistantResponse = chatClient.call(promptWithFunctions).getResult().getOutput();
            conversations.get(session.getId()).add(assistantResponse);
        }

        return assistantResponse.getContent();
    }
}
