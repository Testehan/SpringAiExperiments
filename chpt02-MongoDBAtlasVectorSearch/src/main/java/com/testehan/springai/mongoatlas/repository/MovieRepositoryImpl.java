package com.testehan.springai.mongoatlas.repository;

import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.testehan.springai.mongoatlas.models.Movie;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

// these 2 static imports are available because we used the  <mongodb.version>4.11.0</mongodb.version>
import static com.mongodb.client.model.Aggregates.vectorSearch;
import static com.mongodb.client.model.search.SearchPath.fieldPath;

import static java.util.Arrays.asList;

@Repository
public class MovieRepositoryImpl implements MovieRepository{

    private final MongoDatabase mongoDatabase;

    public MovieRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    @Override
    public Flux<Movie> findMoviesByVector(List<Double> embedding) {
        String indexName = "vector_index";
        int numCandidates = 100;  // how many neighbours it will use when doing the nearest neighbour search; it should be
                                  // higher than the limit we set below...higher numbers of this variable will
                                 // provide higher accuracy, but it will also give some latency hits..
        int limit = 5;

        // right now we only include the vector search, but you can include more (i guess more fields to verify etc)
        // but this will take longer to compute and it will get you better results
        List<Bson> pipeline = asList(
                vectorSearch(
                        fieldPath("plot_embedding"),
                        embedding,
                        indexName,
                        numCandidates,
                        limit));

        return Flux.from(getMovieCollection().aggregate(pipeline, Movie.class));
    }

    private MongoCollection<Movie> getMovieCollection() {
        return mongoDatabase.getCollection("embedded_movies", Movie.class);
    }
}
