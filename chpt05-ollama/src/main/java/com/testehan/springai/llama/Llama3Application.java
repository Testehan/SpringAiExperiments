package com.testehan.springai.llama;

import com.testehan.springai.llama.functions.WeatherConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// below annotation is needed so that the properties defined in WeatherConfigProperties are
// configured and injectable
@EnableConfigurationProperties(WeatherConfigProperties.class)
@SpringBootApplication
public class Llama3Application {

    public static void main(String[] args) {
        SpringApplication.run(Llama3Application.class, args);
    }

}