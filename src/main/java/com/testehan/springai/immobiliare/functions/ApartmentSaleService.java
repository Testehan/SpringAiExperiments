package com.testehan.springai.immobiliare.functions;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.*;
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
    @JsonClassDescription("Get apartments for sale based on provided criteria")
    public record Request(
            @JsonProperty(required = true, value = "location") @JsonPropertyDescription("The location") String location,
            @JsonProperty(value = "price") @JsonPropertyDescription("The price") Long price,
            @JsonProperty(value = "surface") @JsonPropertyDescription("The surface") Integer surface) {
    }
                        // todo response should be "Apartments information"
    public record Response(List<String> information) {
    }

//    @Override
//    public Response apply(Request request) {
//
//        // todo maybe here i can just query a database...
//        var location = request.location();
//        var filtersMap = extractFilters(request);
//
//        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
//        StringBuilder sb = new StringBuilder();
//        for (Map.Entry<String, Object> entry : filtersMap.entrySet()){
//            String operation = " == ";//getOperation(entry);
//            sb.append(entry.getKey());
//            sb.append(operation);
//            sb.append(entry.getValue());
//            sb.append(" AND ");
//        }
//
//
//        if (!Strings.isBlank(location)) {
//
//            List<Document> similarDocuments = immobiliareVectorStore.similaritySearch(SearchRequest.query(location).withTopK(2));
//            List<String> contentList = similarDocuments.stream().map(Document::getContent).toList();
//
//
//
//
////            PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
////            Map<String, Object> promptParameters = new HashMap<>();
////            promptParameters.put("location", location);
////            promptParameters.put("documents", String.join("\n", contentList));
////
////            var outputParser = new BeanOutputParser<>(Apartments.class);
////            String format = outputParser.getFormat();
////            Apartments apartmentsFromVector = outputParser.parse(String.join("", contentList));
////            System.out.println("format = " + format);
////            promptParameters.put("format", format);
////
////            Prompt prompt = promptTemplate.create(promptParameters);
////
////            Generation generation = chatClient.call(prompt).getResult();
////            Apartments apartments = outputParser.parse(generation.getOutput().getContent());
//
//            return new Response(contentList);
//        } else {
//            return new Response(new ArrayList<>());
//        }
//
//    }

    @Override
    public Response apply(Request request) {

        var filtersMap = extractFilters(request);

        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        filterBuilder.eq("location",request.location);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : filtersMap.entrySet()){
            String operation = " == ";//getOperation(entry);
            sb.append(entry.getKey());
            sb.append(operation);
            sb.append("'" + entry.getValue() + "'");
            sb.append(" AND ");
        }

        var query = sb.toString();
        if (!Strings.isBlank(query)) {

            List<Document> similarDocuments = immobiliareVectorStore.similaritySearch(query);
//            List<Document> similarDocuments = immobiliareVectorStore.similaritySearch(SearchRequest.defaults().withFilterExpression(query).withTopK(2));
            List<String> contentList = similarDocuments.stream().map(Document::getContent).toList();


            return new Response(contentList);
        } else {
            return new Response(new ArrayList<>());
        }

    }

    private Map<String, Object> extractFilters(Request request){
        Map<String,Object> filtersMap = new HashMap<>();

        if (Objects.nonNull(request.location)){
            filtersMap.put("location",request.location);
        }
        if (Objects.nonNull(request.price)){
            filtersMap.put("price",request.price);
        }
        if (Objects.nonNull(request.surface)){
            filtersMap.put("surface",request.surface);
        }

        return filtersMap;
    }

}
