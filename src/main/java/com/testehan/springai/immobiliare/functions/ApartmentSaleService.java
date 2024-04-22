package com.testehan.springai.immobiliare.functions;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


@Component
public class ApartmentSaleService implements Function<ApartmentSaleService.Request, ApartmentSaleService.Response> {

    private final ChatClient chatClient;

    private final VectorStore immobiliareVectorStore;

    @Value("classpath:/prompts/rag-prompt-template-apartments-sale.txt")
    private Resource ragPromptTemplate;

    public ApartmentSaleService(ChatClient chatClient, VectorStore immobiliareVectorStore) {
        this.chatClient = chatClient;
        this.immobiliareVectorStore = immobiliareVectorStore;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("Get the apartments for sale from location")
    public record Request(
            @JsonProperty(required = true, value = "location") @JsonPropertyDescription("The city") String location) {
    }
                        // todo response should be "Apartments information"
    public record Response(List<String> information) {
    }

    @Override
    public Response apply(Request request) {

        // todo maybe here i can just query a database...
        var location = request.location();
        if (!Strings.isBlank(location)) {

            List<Document> similarDocuments = immobiliareVectorStore.similaritySearch(SearchRequest.query(location).withTopK(2));
            List<String> contentList = similarDocuments.stream().map(Document::getContent).toList();

//            PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
//            Map<String, Object> promptParameters = new HashMap<>();
//            promptParameters.put("location", location);
//            promptParameters.put("documents", String.join("\n", contentList));
//
//            var outputParser = new BeanOutputParser<>(Apartments.class);
//            String format = outputParser.getFormat();
//            Apartments apartmentsFromVector = outputParser.parse(String.join("", contentList));
//            System.out.println("format = " + format);
//            promptParameters.put("format", format);
//
//            Prompt prompt = promptTemplate.create(promptParameters);
//
//            Generation generation = chatClient.call(prompt).getResult();
//            Apartments apartments = outputParser.parse(generation.getOutput().getContent());

            return new Response(contentList);
        } else {
            return new Response(new ArrayList<>());
        }

    }

}
