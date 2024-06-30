package com.testehan.springai.immobiliare.functions;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.util.function.Function;

// TODO this does not work very well...seems like the LLM almost chooses randomly when to call this ...even if i specify
// that i do not want an llm...

@Component
public class EmailApartmentsService implements Function<EmailApartmentsService.Request, EmailApartmentsService.Response> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("Send me an email with the apartments information")
    public record Request(
            @JsonProperty(required = true, value = "userEmail") @JsonPropertyDescription("The user Email address") String userEmail){

    }

    public record Response(String responseInfo) {
    }


    @Override
    public Response apply(Request request) {

        var userEmail = request.userEmail;
        if (Strings.isNotBlank(userEmail)) {
            System.out.println("Email sent to user with email " + userEmail);
            return new Response("Email sent !");
        } else {
            System.out.println("Something went wrong and the email was not sent");
            return new Response("Error - Email NOT sent !");
        }
    }

}
