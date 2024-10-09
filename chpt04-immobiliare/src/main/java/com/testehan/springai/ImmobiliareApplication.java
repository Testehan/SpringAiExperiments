package com.testehan.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
	Below is the approach when letting the LLM use the functions defined
		In order to send requests, use the collection "Immobiliare" from Postman.
		Most important request is /decide:
			- /decide?message=show me apartments for sale in Marasti
				will return a json of the top 2 similarity search for apartments in Marasti
			- /decide?message=Send me an email with the apartments info
				will send an email or ask for email (you should provide email from code, since eventually,
				users using this app will be logged in)
			- /decide?message=what is the weather like in Cluj-Napoca
				will call another function that tells you the weather in Cluj

	Here is the UI usage of the app:
		1. start app
		2. go to http://localhost:8080/
		3. start answering questions and getting results

*/

@SpringBootApplication
@EnableScheduling
public class ImmobiliareApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImmobiliareApplication.class, args);
	}

}
