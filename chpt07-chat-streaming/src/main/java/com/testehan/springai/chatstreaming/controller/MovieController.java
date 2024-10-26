package com.testehan.springai.chatstreaming.controller;

import com.testehan.springai.chatstreaming.model.Movie;
import com.testehan.springai.chatstreaming.service.MovieApiService;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Controller
public class MovieController {
    private final MovieApiService movieApiService;

    public MovieController(MovieApiService movieApiService) {
        this.movieApiService = movieApiService;
    }

    @GetMapping("/movies")
    public String getMoviesPage(Model model) {
        return "movies"; // Serve the initial Thymeleaf template
    }

    @GetMapping(value = "/movies/stream", produces = "text/event-stream")
    @ResponseBody
    public Flux<String> streamMovies(@RequestParam String message, ServerHttpResponse response) {
        response.getHeaders().set("Transfer-Encoding", "chunked");
        response.getHeaders().set("Content-Type", "text/event-stream");
        response.getHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");

        List<Movie> sampleMovies = Arrays.asList(
                new Movie("Inception", 2010, "A mind-bending thriller."),
                new Movie("The Matrix", 1999, "A computer hacker learns about the true nature of reality."),
                new Movie("Interstellar", 2014, "A team of explorers travel through a wormhole in space.")
        );

        return Flux.fromIterable(sampleMovies)
                .delayElements(Duration.ofSeconds(1))  // Delay for testing purposes
                .map(movie -> formatMovieHtml(movie) ); //+ "\n\n"

//        return movieApiService.streamMovies()
//                .map(movie -> formatMovieHtml(movie) + "\n\n");
    }

    private String formatMovieHtml(Movie movie){
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        var div = "<div class='movie'>" +
                "<h2>" + movie.title() + " (" + movie.year() + ")</h2>" +
                "<p>" + movie.plot() + "</p>" +
                "</div>";
        System.out.println(div);

        return div;
    }
}
