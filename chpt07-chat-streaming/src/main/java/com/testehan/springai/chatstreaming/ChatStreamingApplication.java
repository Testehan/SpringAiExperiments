package com.testehan.springai.chatstreaming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;


@SpringBootApplication
public class ChatStreamingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatStreamingApplication.class, args);
	}

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	RestClient.Builder restClientBuilder() {
		RestClientBuilderConfigurer configurer = new RestClientBuilderConfigurer();
		RestClient.Builder builder = RestClient.builder().requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS));
		return configurer.configure(builder);
	}

}
