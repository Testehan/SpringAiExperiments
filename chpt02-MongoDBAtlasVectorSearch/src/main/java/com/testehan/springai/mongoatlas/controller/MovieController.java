package com.testehan.springai.mongoatlas.controller;

import com.testehan.springai.mongoatlas.models.Movie;
import com.testehan.springai.mongoatlas.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class MovieController {

    private final MovieService movieService;

    @Autowired
    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/movies/semantic-search")
    public Mono<List<Movie>> performSemanticSearch(@RequestParam("plotDescription") String plotDescription) {
        return movieService.getMoviesSemanticSearch(plotDescription);
    }



}
