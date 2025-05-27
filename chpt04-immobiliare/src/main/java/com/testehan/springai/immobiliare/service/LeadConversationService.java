package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.LeadConversation;
import com.testehan.springai.immobiliare.model.MessageType;
import com.testehan.springai.immobiliare.repository.LeadConversationRepository;
import com.testehan.springai.immobiliare.util.FormattingUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeadConversationService {

    private final LeadConversationRepository leadConversationRepository;
    private final FormattingUtil formattingUtil;

    public LeadConversationService(LeadConversationRepository leadConversationRepository, FormattingUtil formattingUtil) {
        this.leadConversationRepository = leadConversationRepository;
        this.formattingUtil = formattingUtil;
    }

    public void saveConversationTextMessage(String waUserId, String messageId, String text, MessageType direction) {
        if (!leadConversationRepository.existsByMessageId(messageId)) {
            LeadConversation message = new LeadConversation();
            message.setWaUserId(waUserId);
            message.setMessageId(messageId);
            message.setDirection(direction);
            message.setText(text);
            message.setTimestamp(formattingUtil.getFormattedDateCustom(LocalDateTime.now()));

            leadConversationRepository.save(message);
        }
    }

    public String getConversation(String phoneNumberInInternational){
        var phoneWithNoPrecedingPlus = phoneNumberInInternational.substring(1);
        return formatConversation(leadConversationRepository.findByWaUserIdOrderByTimestampAsc(phoneWithNoPrecedingPlus));
    }

    public String formatConversation(List<LeadConversation> messages) {
        StringBuilder conversation = new StringBuilder();
        for (LeadConversation message : messages) {
            String speaker = message.getDirection() == MessageType.RECEIVED ? "User" : "Agent";
            conversation.append(speaker)
                    .append(": ")
                    .append(message.getText())
                    .append("\n");
        }
        return conversation.toString();
    }
}
