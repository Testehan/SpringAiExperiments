package com.testehan.springai.immobiliare.configuration;

import org.springframework.ai.reader.JsonMetadataGenerator;

import java.util.HashMap;
import java.util.Map;

public class ImmobiliareMetadataGenerator implements JsonMetadataGenerator {
    @Override
    public Map<String, Object> generate(Map<String, Object> jsonMap) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", jsonMap.get("name"));
        metadata.put("location", jsonMap.get("location"));
        metadata.put("price", jsonMap.get("price"));
        metadata.put("surface", jsonMap.get("surface"));
        metadata.put("noOfRooms", jsonMap.get("noOfRooms"));

        return metadata;
    }
}
