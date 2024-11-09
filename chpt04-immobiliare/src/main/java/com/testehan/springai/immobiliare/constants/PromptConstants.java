package com.testehan.springai.immobiliare.constants;

public class PromptConstants {
    public static final String M00_IRRELEVANT_PROMPT = "The prompt provided is not relevant for this application. Try and provide something related to real estate";
    public static final String M00_NO_SEARCH_QUERIES_AVAILABLE = "You do not have search queries available :(.";
    public static final String M01_INITIAL_MESSAGE = "Hi..are you interested in apartments for rent or sale ?";
    public static final String M02_CITY = "Which city are you interested in ?";
    public static final String M021_SUPPORTED_CITIES = "Supported cities right now are only: %s ";
    public static final String M03_DETAILS = "You are looking for properties for %s in %s. Give me more details about the location you are searching for.";
    public static final String M04_APARTMENTS_FOUND = "Searching for properties based on the provided description. Click on 'Show Contact' to see the owners phone number :).";
    public static final String M04_NO_APARTMENTS_FOUND = "No apartments found with the given criteria. Please provide another description or type 'restart' to start from the beginning.";
}
