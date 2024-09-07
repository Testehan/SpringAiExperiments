package com.testehan.springai.immobiliare.advisor;

import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

@FunctionalInterface
public interface MemoryBasisExtractor {
    List<Message> extract(AdvisedRequest request);
}
