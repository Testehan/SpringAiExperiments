package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.LeadConversation;
import com.testehan.springai.immobiliare.model.MessageType;
import com.testehan.springai.immobiliare.repository.LeadConversationRepository;
import com.testehan.springai.immobiliare.util.FormattingUtil;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public boolean doWeNeedToContinueConversation(String phoneNumberInInternational){
        var phoneWithNoPrecedingPlus = phoneNumberInInternational.substring(1);
        List<LeadConversation> messages = leadConversationRepository.findByWaUserIdOrderByTimestampAsc(phoneWithNoPrecedingPlus);

        if (messages == null || messages.size() == 0) {
            //no conversation so far, means that we need to start talking
            return true;
        }

        // if the last message in the conversation is from the user, we must evaluate the conversation
        if (messages.getLast().getDirection().equals(MessageType.RECEIVED)) {
            return true;
        } else {
            // if the last message, was 2 days ago, no matter who sent it, we need to reevaluate the conversation and
            // possible send a reminder
            if (isMoreThanTwoDaysAgo(messages.getLast().getTimestamp()) && !lastTwoMessagesWereSent(messages)){
                return true;
            }
        }
        return false;
    }

    public String formatConversation(List<LeadConversation> messages) {
        StringBuilder conversation = new StringBuilder();
        for (LeadConversation message : messages) {
            String speaker = message.getDirection() == MessageType.RECEIVED ? "User" : "Agent";
            conversation.append(message.getTimestamp() + " ")
                    .append(speaker)
                    .append(": ")
                    .append(message.getText())
                    .append("\n");
        }
        return conversation.toString();
    }

    private boolean isMoreThanTwoDaysAgo(String lastMessageTimestamp) {
        // Parse the timestamp string to LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime messageTime = LocalDateTime.parse(lastMessageTimestamp, formatter);

        // Get current local datetime
        LocalDateTime now = LocalDateTime.now();

        // Calculate duration between message time and now
        Duration duration = Duration.between(messageTime, now);

        // Check if duration is greater than 2 days (48 hours)
        return duration.toHours() > 48;
    }

    private boolean lastTwoMessagesWereSent(List<LeadConversation> messages) {
        if (messages.size() >= 2){
            return messages.getLast().getDirection().equals(MessageType.SENT) &&
                    messages.get(messages.size() - 2).getDirection().equals(MessageType.SENT);
        } else {
            return false;
        }
    }

    public void deleteByWaUserId(String waUserId){
        leadConversationRepository.deleteByWaUserId(waUserId);
    }
}
