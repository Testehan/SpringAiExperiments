package com.testehan.springai.immobiliare.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document(collection = "listing_statistics")
public class ListingStatistics {

    @Id
    private String inputHash;
    private String city;
    private String propertyType;
    private Integer noOfRooms;

    private Integer contactThreshold;
    private Integer favoriteThreshold;

}
