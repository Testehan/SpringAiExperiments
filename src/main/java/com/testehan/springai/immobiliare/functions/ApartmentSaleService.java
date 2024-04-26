package com.testehan.springai.immobiliare.functions;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;


@Component
public class ApartmentSaleService implements Function<ApartmentSaleService.Request, ApartmentSaleService.Response> {

    private final VectorStore immobiliareVectorStore;

    public ApartmentSaleService(VectorStore immobiliareVectorStore) {
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

    @Override
    public Response apply(Request request) {

        var query = getQueryFromRequest(request);
        if (!Strings.isBlank(query)) {

            List<Document> similarDocuments = immobiliareVectorStore.similaritySearch(query);
            // TODO the reason why the call from below fails, is because the implementation SimpleVectorStore does
            // not provide the functionality to filter metadata ..As the documentation says,
            // "SimpleVectorStore - A simple implementation of persistent vector storage, good for educational purposes."
            // so before getting into production, one would need to research a proper vectorestore
//            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
//            Filter.Expression location = filterBuilder.eq("location", request.location).build();
//            List<Document> similarDocuments = immobiliareVectorStore.similaritySearch(SearchRequest.defaults().withFilterExpression(location).withTopK(2));


            List<String> contentList = similarDocuments.stream().map(Document::getContent).toList();
            return new Response(contentList);
        } else {
            return new Response(new ArrayList<>());
        }

    }

    private String getQueryFromRequest(Request request) {
        var filtersMap = extractFilters(request);

        StringBuilder sb = new StringBuilder();
        var operation = " is ";
        for (Map.Entry<String, Object> entry : filtersMap.entrySet()){
            sb.append(entry.getKey());
            sb.append(operation);
            sb.append("'" + entry.getValue() + "'");
            sb.append(" AND ");
        }

        return sb.toString();
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
