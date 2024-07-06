package com.testehan.springai.mongoatlas.service;

import com.testehan.springai.mongoatlas.models.Movie;
import com.testehan.springai.mongoatlas.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final OpenAiService embedder;

    @Autowired
    public MovieService(MovieRepository movieRepository, OpenAiService embedder) {
        this.movieRepository = movieRepository;
        this.embedder = embedder;
    }

    public Mono<List<Movie>> getMoviesSemanticSearch(String plotDescription) {
        return embedder.createEmbedding(plotDescription)
                .flatMapMany(movieRepository::findMoviesByVector)
                .collectList();
    }

    // todo find a better name if this will work
    public Flux<Movie> getMoviesSemanticSearch2(String plotDescription) {
        return embedder.createEmbedding(plotDescription)
                .flatMapMany(movieRepository::findMoviesByVector);
    }
}
