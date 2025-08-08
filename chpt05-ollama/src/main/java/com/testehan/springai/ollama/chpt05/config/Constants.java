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

    public static final String PROMPT_VERIFY_LISTING = """
        Ești un expert în verificarea consistenței datelor extrase din texte.
    
        Primești două intrări:
        1. Textul original (nestructurat): {rawText}
    
        2. Obiectul JSON generat (presupus extragerea ta): {jsonListing}
    
        Sarcina ta este să verifici dacă informațiile din JSON sunt:
        - corect extrase din textul original,
        - complete (nu lipsesc detalii importante),
        - fidele (nu au fost modificate sau inventate).
        
        Foarte important :
            - Consideră JSON-ul ca fiind *acceptabil* dacă cel puțin 95% din informații corespund textului.
            - Raportează doar abaterile semnificative sau erorile evidente.
    
        ### Reguli de validare
    
        1. **Corectitudine factuală:**
           - Fiecare valoare din JSON trebuie să apară explicit în textul original sau să poată fi dedusă clar (ex: „2 camere” ↔ „apartament cu 2 camere”).
           - Dacă o informație din JSON NU apare în text, marcheaz-o drept *inventată*.
           - Dacă lipsește o informație importantă din text (dar ar fi trebuit extrasă conform contextului), marcheaz-o drept *omisă*.
           - Dacă valoarea este parțial corectă, dar interpretată greșit (ex: „etaj 4” în loc de „etaj 2/4”), marcheaz-o drept *incorectă*.
    
        2. **Completitudine:**
           - Verifică dacă `shortDescription` include toate detaliile menționate în textul original (suprafață, dotări, etaj, mobilier, condiții etc.).
           - Nu trebuie să lipsească niciun detaliu clar menționat în text.
           - Reformulările sunt permise, dar nu omisiunile.
    
        3. **Fidelitate:**
           - JSON-ul nu trebuie să conțină date inventate (ex: an construcție, suprafață, zonă, facilități inexistente în text).
    
        4. **Localizare:**
           - `city` trebuie să provină din text (dacă apare „Cluj”, acceptă „Cluj-Napoca”).
           - `area` trebuie să corespundă unei locații menționate (stradă, cartier, zonă, reper). Dacă nu apare în text, nu ar trebui să existe în JSON.
    
        ### Formatul răspunsului
    
        Returnează un obiect JSON care evaluează fiecare câmp în parte, cu următoarea structură: {format}
        Campul isConsistent trebuie sa fie true dacă cel puțin 95% din informații corespund textului, sau false daca mai putin.
        
        Nu reformula și nu corecta JSON-ul. Doar analizează fidelitatea datelor față de textul original și raportează abaterile.
        """;

    public static final String PROMPT_CORRECT_LISTING = """
        Ai generat un JSON cu probleme de consistență față de textul original.
        Te rog să îl corectezi conform feedbackului de mai jos, fără să modifici câmpurile care sunt corecte.
        Feedback: {feedback}
        JSON original: {originalJson}
        Text original: {rawText}
        Returnează DOAR JSON-ul corectat, avand formatul {format}, fără alte explicații. 
        """;
}
