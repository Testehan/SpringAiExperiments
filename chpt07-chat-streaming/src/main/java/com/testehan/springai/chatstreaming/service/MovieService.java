package com.testehan.springai.chatstreaming.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testehan.springai.chatstreaming.model.Movie;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class MovieService {

    private final ObjectMapper objectMapper;

    public MovieService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Flux<Movie> transformToMovies(Flux<String> stringFlux) {
        return stringFlux
                // Accumulate characters into a full JSON string
                .scan(new StringBuilder(), (acc, next) -> acc.append(next))  // Append characters
                .filter(buffer -> containsCompleteJsonObject(buffer.toString()))  // Check if the buffer contains complete JSON
                .flatMap(this::splitAndConvertToMovies);  // Convert JSON string to Movie
    }

    // Helper method to convert a JSON string into a Movie object
    private Movie convertToMovie(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, Movie.class);  // Deserialize JSON to Movie
        } catch (Exception e) {
            // Handle any parsing errors (e.g., log and skip the faulty entry)
            throw new RuntimeException("Error parsing movie JSON: " + jsonString, e);
        }
    }

    // Helper method to check if we have at least one complete JSON object in the buffer
    private boolean containsCompleteJsonObject(String jsonString) {
        String trimmed = jsonString.trim();
        // Return true if we have a valid JSON object enclosed in curly braces
        int openBraces = 0;
        int closeBraces = 0;

        for (char c : trimmed.toCharArray()) {
            if (c == '{') openBraces++;
            if (c == '}') closeBraces++;
        }

        // We have a complete object if we have matched curly braces
        return openBraces > 0 && openBraces == closeBraces;
    }

    // Helper method to split the JSON array into individual movie objects and parse them
    private Flux<Movie> splitAndConvertToMovies(StringBuilder jsonArrayString) {
        // Remove the array brackets if necessary and split into individual objects
        String trimmed = jsonArrayString.toString().trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1); // Remove starting bracket
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1); // Remove ending bracket
        }

        // Now split by individual JSON objects (movies), assuming each one is separated by a comma
        String[] movieStrings = trimmed.split("(?<=\\})\\s*,\\s*(?=\\{)");  // Split on commas between objects

        return Flux.fromArray(movieStrings) // Convert the array to Flux<String>
                .map(this::convertToMovie);  // Map each string to a Movie object
    }
}
