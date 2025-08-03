package com.testehan.springai.ollama.chpt05.config;

public class Constants {

    public static final String JSON_SCHEMA_LISTING = """
        {
          "type": "object",
          "description": "Schema for the extracted property information.",
          "properties": {
            "name": {
              "type": "string",
              "description": "Name of the listing."
            },
            "city": {
              "type": "string",
              "description": "City where the apartment is located."
            },
            "area": {
              "type": "string",
              "description": "This must contain the address if available. If address is not mentioned use area where the apartment is located."
            },
            "shortDescription": {
              "type": "string",
              "description": "The apartment description."
            },
            "price": {
              "type": "integer",
              "description": "Price of the apartment. I only want the number, not the currency."
            },
            "surface": {
              "type": "integer",
              "description": "Surface area of the apartment in square meters."
            },
            "noOfRooms": {
              "type": "integer",
              "description": "Number of rooms in the apartment."
            },
            "floor": {
              "type": "string",
              "description": "Floor of the apartment."
            },
            "ownerName": {
              "type": "string",
              "description": "Name of the owner."
            },
            "imageUrls": {
              "type": "array",
              "description": "A list of all found image URLs from the page.",
              "items": {
                "type": "string"
              }
            }
          },
          "required": [
            "name",
            "city",
            "area",
            "shortDescription",
            "price",
            "surface",
            "noOfRooms",
            "floor",
            "ownerName",
            "imageUrls"
          ]
        }
        """;

    public static final String PROMPT_FORMAT_LISTING = "You are a JSON formatting expert. You will receive raw text %s " +
            "Do not try to scrape or browse anything. Do not generate data Your only task is to convert the provided text into a valid JSON object that adheres to the provided schema.\n" +

            "Very important : the city field must contain the name of a valid city from Romania. If the value is Cluj then that is not" +
            "a valid city name. Replace it with Cluj-Napoca." +

            "When generating the 'name' field:\n" +
            "- Rewrite the name to sound professional, concise, and attractive.\n" +
            "- Do NOT include words like 'Proprietar' or 'PF' or 'inchiriez' or 'de inchiriat' since my platform only handles rentals from owners.\n" +
            "- Highlight the number of rooms, surface area, and area if available.\n" +
            "- Keep it under 120 characters.\n" +
            "- Use natural Romanian phrasing for listings (e.g. '2 camere, 90 mp, ultracentral').\n" +

            "When generating the 'shortDescription' field:\n" +
            "- Rewrite the text in Romanian to be clean, professional, and easy to read.\n" +
            "- Keep all factual information (surface, rooms, location, equipment, conditions, etc.).\n" +
            "- Remove redundant parts like 'PF', 'persoana fizica', 'dau în chirie', or 'nu colaborez cu agenții'.\n" +
            "- Organize information into 2–3 short paragraphs.\n" +
            "- Use complete sentences and a natural tone.\n" +
            "- Do NOT add or invent details not present in the input.\n\n" +

            "When generating the 'area' field:\n" +
            "- Extract the most precise available location information from the text.\n" +
            "- It can be a **street name**, **neighborhood**, **square**, or **known nearby landmark** (e.g. university, mall, metro station).\n" +
            "- Follow this priority order: exact address/street > neighborhood > well-known nearby point of interest.\n" +
            "- Example: if the text mentions 'strada Mureșului' and 'aproape de Iulius Mall', return 'strada Mureșului'.\n" +
            "- If no street is given but a landmark is, return that landmark (e.g. 'lângă FSEGA').\n" +
            "- Keep it short and natural (avoid full sentences, just the place name).\n" +
            "- Do NOT repeat the city name in this field.\n\n" +

            "HERE IS THE SCHEMA DEFINITION YOUR FINAL JSON STRING MUST ADHERE TO: \n" +
            "--- SCHEMA START --- \n" +
            JSON_SCHEMA_LISTING + "\n" +
            "--- SCHEMA END --- \n\n" +

            "Based *only* on the provided content and the schema definition, generate the final JSON string." +
            "VERY IMPORTANT : Your final answer MUST be ONLY the raw JSON string. Do not wrap it in markdown or add any other text. Your entire output must start with `{` and end with `}`.";

}
