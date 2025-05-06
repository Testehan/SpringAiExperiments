package com.testehan.springai.immobiliare.configuration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.util.MimeType;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoDBConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBConfig.class);

    @Value("${spring.data.mongodb.uri}")
    private String MONGO_DB_URI;

    @Value("${spring.data.mongodb.database}")
    private String MONGO_DB_NAME;

    private final Converter<MimeType, String> libraryMimeTypeToStringConverter;
    private final Converter<String, MimeType> libraryStringToMimeTypeConverter;

    public MongoDBConfig(
            @Qualifier("mimeTypeToStringConverter") Converter<MimeType, String> mimeTypeToStringConverter,
            @Qualifier("stringToMimeTypeConverter") Converter<String, MimeType> stringToMimeTypeConverter) {
        this.libraryMimeTypeToStringConverter = mimeTypeToStringConverter;
        this.libraryStringToMimeTypeConverter = stringToMimeTypeConverter;
    }

    @Bean
    public MongoClient mongoClient() {
        // THIS is needed to be able to convert data from BSON (meaning from the MongoDB) to POJOs
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(
                        PojoCodecProvider.builder().automatic(true).build()
                )
        );

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(MONGO_DB_URI))
                .codecRegistry(pojoCodecRegistry)
                .build();

        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient) {
        LOGGER.info("Using {}", MONGO_DB_NAME);
        return mongoClient.getDatabase(MONGO_DB_NAME);
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        List<Converter<?, ?>> allConverters = new ArrayList<>();

        allConverters.add(libraryMimeTypeToStringConverter);
        allConverters.add(libraryStringToMimeTypeConverter);

        return new MongoCustomConversions(allConverters);
    }


}
