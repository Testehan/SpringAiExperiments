package com.testehan.springai.chatstreaming.service;

import com.testehan.springai.chatstreaming.model.Movie;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.parser.BeanOutputParser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
public class MovieApiService {

    private final ChatClient chatClient;
    private final MovieService movieService;

    public MovieApiService(ChatClient.Builder builder, MovieService movieService) {
        this.chatClient = builder.build();
        this.movieService = movieService;
    }

    public Flux<Movie> streamMovies() {
        var outputParser = new BeanOutputParser<>(Movie.class);
        String format = outputParser.getFormat();
        System.out.println("format = " + format);

        var promptMessage = "give me exactly 5 movies by actor Al Pacino without duplicated please" + "\n {format}";
        PromptTemplate promptTemplate = new PromptTemplate(promptMessage, Map.of("format", format));
        Prompt prompt = promptTemplate.create();

        Flux<String> movieInformationFlux =  chatClient.prompt(prompt)
                .stream().content();

        return movieService.transformToMovies(movieInformationFlux);
    }
}
