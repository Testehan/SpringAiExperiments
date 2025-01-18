package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.configuration.BeanConfig;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

// TODO Before releasing the app you need to make sure that the SNS service is not in "sandbox" mode
// and that you can send sms to anyone. For this follow the steps from AWS web console
// TODO in order to have the functionality of the users reply to the sms with yes/no..and to get that reply..
// you would need to have => "1. Set Up a Dedicated Phone Number
//Requirement: You'll need either a long code or a short code from Amazon SNS to receive SMS messages.
//Request a dedicated number (long code or short code) via AWS Support, as shared numbers do not support inbound messages.
//A short code is recommended for high-volume traffic."

@Service
public class SmsService {

    private final SnsClient snsClient;

    public SmsService(BeanConfig beanConfig) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(beanConfig.getAwsAccessKeyId(), beanConfig.getAwsAccessSecret());

        snsClient = SnsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(beanConfig.getRegionName()))
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
