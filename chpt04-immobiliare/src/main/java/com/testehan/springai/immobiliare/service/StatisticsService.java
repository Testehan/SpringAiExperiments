package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ListingStatistics;
import com.testehan.springai.immobiliare.util.HashUtil;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticsService {

    private final MongoTemplate mongoTemplate;
    private final HashUtil hashUtil;

    public StatisticsService(MongoTemplate mongoTemplate, HashUtil hashUtil) {
        this.mongoTemplate = mongoTemplate;
        this.hashUtil = hashUtil;
    }

    public void computeAndStoreStatistics() {
        // Group listings by location, rent/sale, and rooms
        // We are collecting all noOfContact and noOfFavourite values into arrays: contacts and favorites
        GroupOperation groupByCategory = Aggregation.group("city", "propertyType", "noOfRooms")
                .push("noOfContact").as("contacts")
                .push("noOfFavourite").as("favorites");

        // Compute the 75th percentile using array slicing
        ProjectionOperation computePercentiles = Aggregation.project()
                .and("_id.city").as("city")
                .and("_id.propertyType").as("propertyType")
                .and("_id.noOfRooms").as("noOfRooms")
                .and(AggregationExpression.from(MongoExpression.create("{$multiply: [{$avg: '$contacts'}, 0.75] }")))
                .as("contactThreshold")
                .and(AggregationExpression.from(MongoExpression.create("{ $multiply: [{$avg: '$favorites'}, 0.75] }")))
                .as("favoriteThreshold");

        // Run the aggregation
        Aggregation aggregation = Aggregation.newAggregation(groupByCategory, computePercentiles);
        List<ListingStatistics> statistics = mongoTemplate.aggregate(aggregation, Apartment.class, ListingStatistics.class).getMappedResults();

        // Store results in global_statistics
        for (ListingStatistics stat : statistics) {
            String hashKey = hashUtil.hashText(stat.getCity() + ":" + stat.getPropertyType() + ":" + stat.getNoOfRooms());

            // Create a new instance (since we can't set ID after creation)
            ListingStatistics newStat = new ListingStatistics(
                    hashKey,
                    stat.getCity(),
                    stat.getPropertyType(),
                    stat.getNoOfRooms(),
                    stat.getContactThreshold(),
                    stat.getFavoriteThreshold()
            );

            mongoTemplate.save(newStat);
        }
    }
}

