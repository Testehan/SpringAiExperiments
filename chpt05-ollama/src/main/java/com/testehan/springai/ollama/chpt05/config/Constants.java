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
        Foarte important: Textul furnizat ca input este in limba romana. Textul furnizat la final trebuie sa fie tot in limba romana !!!
        Foarte important: câmpul „city” trebuie să conțină numele unui oraș valid din România. Dacă valoarea este Cluj, acesta nu este un nume de oraș valid. Înlocuiește-l cu Cluj-Napoca.
    
        La formatarea câmpului „name”:
        - Rescrie numele astfel încât să sune profesionist, concis și atractiv.
        - NU include cuvinte precum „Proprietar”, „PF”, „închiriez” sau „de închiriat”, deoarece platforma mea gestionează doar închirieri de la proprietari.
        - Evidențiază numărul de camere, suprafața și zona, dacă sunt disponibile.
        - Păstrează-l sub 70 de caractere.
        - NU adăuga sau inventa detalii care nu sunt prezente în textul de intrare.
    
        La formatarea câmpului „shortDescription”:
        - Păstrează toate informațiile faptice (suprafață, camere, locație, echipamente, condiții etc.).
        - Elimină părțile redundante precum „PF”, „persoană fizică”, „dau în chirie” sau „nu colaborez cu agenții”.
        - Organizează informațiile în 2–3 paragrafe scurte.
        - Folosește propoziții complete și un ton natural.
        - NU adăuga sau inventa detalii care nu sunt prezente în textul de intrare.
        - Nu adauga referinte la platforme imobiliare gen olx publi24 etc
    
        La formatarea câmpului „area”:
        - Extrage din text cele mai precise informații disponibile despre locație.
        - Poate fi un **nume de stradă**, **cartier**, **piață** sau **punct de reper cunoscut din apropiere** (de ex. universitate, mall, stație de metrou).
        - Urmează această ordine de prioritate: adresă exactă/stradă > cartier > punct de interes cunoscut din apropiere.
        - Exemplu: dacă textul menționează „strada Mureșului” și „aproape de Iulius Mall”, returnează „strada Mureșului”.
        - Dacă nu este dată nicio stradă, dar este menționat un punct de reper, returnează acel punct de reper (de ex. „lângă FSEGA”).
        - Păstrează-l scurt și natural (evită propozițiile complete, doar numele locului).
        - NU repeta numele orașului în acest câmp.
    
        Iată DEFINIȚIA SCHEMEI PE CARE ȘIRUL TĂU JSON FINAL TREBUIE SĂ O RESPECTE:
        {format}
        
        Bazat *doar* pe conținutul furnizat și pe definiția schemei, generează șirul JSON final.
        FOARTE IMPORTANT: Răspunsul tău final TREBUIE să fie NUMAI șirul JSON brut. Nu-l încadra în markdown și nu adăuga niciun alt text.
        """;


}
