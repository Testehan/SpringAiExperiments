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

    public static final String PROMPT_FORMAT_LISTING = """
        Ești un expert în formatarea JSON. Vei primi un text brut: {rawText}
        Nu încerca să extragi date de pe internet sau să navighezi. Nu genera date. Singura ta sarcină este să convertești textul furnizat într-un obiect JSON valid care respectă schema furnizată.
        Textul furnizat ca input este în limba română. Textul furnizat la final trebuie să fie tot în limba română.
        
        Foarte important:
        - Câmpul „city” trebuie să conțină un nume real de oraș din România. Dacă valoarea este „Cluj”, folosește „Cluj-Napoca”.
        - Nu inventa adrese. Dacă nu este menționată o stradă, lasă doar cartierul sau zona (ex: „Nufărul”).
        - Nu adăuga sau inventa detalii care nu sunt prezente în text.
        
        La formatarea câmpului „name”:
        - Rescrie numele astfel încât să sune profesionist, concis și atractiv.
        - NU include cuvinte precum „Proprietar”, „PF”, „închiriez” sau „de închiriat”.
        - Evidențiază numărul de camere, suprafața și zona, dacă sunt disponibile.
        - Păstrează-l sub 70 de caractere.
        - NU adăuga sau inventa detalii care nu sunt prezente în textul de intrare.
        
        La formatarea câmpului „shortDescription”:
        - Include TOATE informațiile utile din textul original, fără a omite detalii.
        - Păstrează detalii despre compartimentare, dotări, suprafață, balcon, an construcție, etaj, mobilier, echipamente, reguli (fumat, animale) și condiții de închiriere.
        - Poți reformula pentru claritate și coerență, dar NU rezuma și NU scurta textul.
        - Menține un ton natural, fluent și complet. Este preferabil ca textul să fie lung, dar informativ.
        - Nu adăuga referințe la platforme imobiliare (ex: OLX, Publi24).
        
        La formatarea câmpului „area”:
        - Extrage cea mai precisă informație de localizare disponibilă.
        - Poate fi numele unei străzi, cartier, piață sau punct de reper cunoscut.
        - Urmează ordinea: adresă exactă > cartier > punct de reper.
        - Dacă nu este menționat nimic, lasă câmpul gol.
        - NU inventa locații inexistente.
        
        Iată DEFINIȚIA SCHEMEI PE CARE ȘIRUL TĂU JSON FINAL TREBUIE SĂ O RESPECTE:
        {format}
        
        ATENȚIE: Descrierea trebuie să fie completă. Nu omite niciun detaliu prezent în textul original, chiar dacă pare minor.
        
        Răspunsul tău final TREBUIE să fie NUMAI șirul JSON brut. Nu-l încadra în markdown și nu adăuga niciun alt text.
        """;



}
