package com.testehan.springai.mongoatlas.repository;

import com.testehan.springai.mongoatlas.models.Movie;
import reactor.core.publisher.Flux;

import java.util.List;

public interface MovieRepository {
    Flux<Movie> findMoviesByVector(List<Double> embedding);
}
