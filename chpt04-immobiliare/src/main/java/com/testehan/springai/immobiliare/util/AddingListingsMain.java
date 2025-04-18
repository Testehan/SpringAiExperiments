package com.testehan.springai.immobiliare.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

class ListingImage {
    @JsonProperty("Extract Date")
    private String extractDate;
    public String imageUrl;
    @JsonProperty("Task Link")
    public String taskLink;
    @JsonProperty("Origin URL")
    public String originUrl;
    @JsonProperty("ListingImages Limit")
    public String listingImagesLimit;
}

class Listing {
    @JsonProperty("Extract Date")
    public String extractDate;
    @JsonProperty("Status")
    public String status;
    @JsonProperty("Task Link")
    public String taskLink;
    @JsonProperty("Origin URL")
    public String originURL;
    @JsonProperty("ListingImages Limit")
    public int listingImagesLimit;
    @JsonProperty("Phone")
    public String phone;
    public String ownerName;
    public String price;
    public String address;
    public String fullDescription;
    public String noOfRooms;
    public String floor;
    public String surface;
    public String fullTitle;
    @JsonProperty("ListingImages")
    public List<ListingImage> listingImages;
}

class Root {
    public List<Listing> data;
    public String table;
    public String schema_version;
    public String export_id;
    public String export_created_at;
}

public class AddingListingsMain {
    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {

            // Create a RestTemplate instance
            RestTemplate restTemplate = new RestTemplate();
            String endpoint = "https://casamia.ai/api/apartments/batchsave";
            String sessionId = "0A86BB3047F1E3758C2202BE9B2FD183";

            Root root = objectMapper.readValue(new File("/Users/danteshte/JavaProjects/spring-ai-experiments/chpt04-immobiliare/src/test/resources/data3.json"), Root.class);
            for (Listing listing : root.data) {
                try {
                    // Prepare the MultipartFile[] for images (if any)
                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

                    body.add("name", listing.fullTitle);
                    body.add("city", "Cluj-Napoca");
                    body.add("area", listing.address);
                    body.add("shortDescription", listing.fullDescription);
                    body.add("price", Integer.valueOf(listing.price));
                    body.add("propertyType", "rent");
                    body.add("surface", Integer.valueOf(listing.surface));
                    body.add("noOfRooms", Integer.valueOf(listing.noOfRooms));
                    body.add("floor", listing.floor);
                    body.add("contact", listing.phone);
                    body.add("ownerName", listing.ownerName);
                    body.add("active", false);
                    body.add("availableFrom", getAvailableFromToday());


                    if (listing.listingImages != null) {
                        int i = 1;
                        for (ListingImage img : listing.listingImages) {
                            try {
                                // Download image from the URL
                                URL url = new URL(img.imageUrl); // The image URL (online)
                                InputStream inputStream = url.openStream();
                                byte[] imageBytes = toByteArray(inputStream); // Convert InputStream to byte array

                                final int j = i;
                                ByteArrayResource fileResource = new ByteArrayResource(imageBytes) {
                                    @Override
                                    public String getFilename() {
                                        return "image" + j + ".jpg"; // or whatever file name and extension you want
                                    }
                                };
                                i++;

                                // Add to the body
                                body.add("apartmentImages", fileResource);
                            } catch (Exception e) {
                                System.out.println("Error processing image " + img.imageUrl);
                                System.out.println(e.getMessage());
                            }
                        }
                    }

                    // Prepare the request
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                    headers.set("Cookie", "JSESSIONID=" + sessionId);
                    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                    // Make the POST request
                    ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, requestEntity, String.class);

                    // Handle the response
                    if (response.getStatusCode() == HttpStatus.OK) {
                        System.out.println("Apartment successfully saved!");
                    } else {
                        System.out.println("Failed to save apartment. Status code: " + response.getStatusCode() + " body: " + response.getBody());
                    }

                    Thread.sleep(40000);
                } catch (HttpClientErrorException e) {
                    System.out.println(e.getMessage());
                }


                System.out.println("-----------------------------");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Helper method to convert InputStream to byte array
    private static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            byteArrayOutputStream.write(bytes, 0, read);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static String getAvailableFromToday(){
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return today.format(formatter);
    }
}
