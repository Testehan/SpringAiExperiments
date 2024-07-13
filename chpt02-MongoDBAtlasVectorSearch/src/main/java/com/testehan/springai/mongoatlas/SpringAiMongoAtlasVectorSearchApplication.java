package com.testehan.springai.mongoatlas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
	Run the application and then:
		1. go to http://localhost:8080/
		2. enter a prompt above a movie...click on search, and then you should get top 5 results
		3. if there are errors during startup, that are related to mongoDB, go to mongoDB webconsole, login with
		google account and go to the Connect tab
		and see if your current ip is set to be able to access the DB. (a popup should appear in the case where your IP
		can't access the DB and a way to remediate the issue)

		3. go to http://localhost:8080/callBySinch
		4. This will cause a call to be made to your phone...Not related to AI necessarily, but still nice to have

*/

@SpringBootApplication
public class SpringAiMongoAtlasVectorSearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiMongoAtlasVectorSearchApplication.class, args);
	}

}
