package com.testehan.springai.immobiliare.functions;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.util.function.Function;


@Component
public class ApartmentRentService implements Function<ApartmentRentService.Request, ApartmentRentService.Response> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("Get the weather in location")
    public record Request(
            @JsonProperty(required = true, value = "message") @JsonPropertyDescription("The city and state") String message) {
    }

    public record Response(String information) {
    }

    @Override
    public Response apply(Request request) {

        String information = "";
        if (request.message().contains("Cluj-Napoca")) {
            information = "15";
        }
        else if (request.message().contains("Tokyo")) {
            information = "10";
        }
        else if (request.message().contains("San Francisco")) {
            information = "30";
        }

        return new Response(information);
    }

}
