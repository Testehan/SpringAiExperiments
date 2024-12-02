package com.testehan.springai.immobiliare.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

// TODO Before releasing the app you need to make sure that the SNS service is not in "sandbox" mode
// and that you can send sms to anyone. For this follow the steps from AWS web console

@Service
public class SmsService {
    private final SnsClient snsClient;

    public SmsService() {
        snsClient = SnsClient.builder()
                .region(Region.EU_NORTH_1) // Choose your region
                .build();
    }

    public void sendSms(String phoneNumber, String message) {
        PublishRequest request = PublishRequest.builder()
                .message(message)
                .phoneNumber(phoneNumber)
                .build();

        snsClient.publish(request);
    }
}
