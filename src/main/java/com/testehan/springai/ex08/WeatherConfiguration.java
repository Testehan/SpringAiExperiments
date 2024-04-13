package com.testehan.springai.ex08;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class WeatherConfiguration {

    @Bean
    @Description("Get the weather in location") // is optional and provides a function description (2) that helps the model to understand when to call the function. It is an important property to set to help the AI model determine what client side function to invoke.
    public Function<MockWeatherService.Request, MockWeatherService.Response> weatherFunction() {
        return new MockWeatherService();
    }
}
