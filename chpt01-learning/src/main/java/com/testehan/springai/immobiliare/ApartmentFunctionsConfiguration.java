package com.testehan.springai.immobiliare;

import com.testehan.springai.immobiliare.functions.ApartmentRentService;
import com.testehan.springai.immobiliare.functions.ApartmentSaleService;
import com.testehan.springai.immobiliare.functions.EmailApartmentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class ApartmentFunctionsConfiguration {

    @Autowired
    private ApartmentRentService apartmentRentService;

    @Autowired
    private ApartmentSaleService apartmentSaleService;
    @Autowired
    private EmailApartmentsService emailApartmentsService;

    @Bean
    @Description("Get weather in location")     //  todo think about a better description when dealing with apartments
    /* is optional and provides a function description (2) that helps the model to
        understand when to call the function. It is an important property to set to help
        the AI model determine what client side function to invoke.
    */
    public Function<ApartmentRentService.Request, ApartmentRentService.Response> apartmentsFunction() {
        return apartmentRentService;
    }

    @Bean
    @Description("Get all apartments for sale based on provided criteria")
    public Function<ApartmentSaleService.Request, ApartmentSaleService.Response> apartmentsSaleFunction() {
        return apartmentSaleService;
    }

    @Bean
    @Description("Send me an email with the apartments information")
    public Function<EmailApartmentsService.Request, EmailApartmentsService.Response> emailApartmentsFunction() {
        return emailApartmentsService;
    }



}
