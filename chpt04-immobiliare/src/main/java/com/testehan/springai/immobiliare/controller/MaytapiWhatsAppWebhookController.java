package com.testehan.springai.immobiliare.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.testehan.springai.immobiliare.model.MessageType;
import com.testehan.springai.immobiliare.service.LeadConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/maytapiwhatsapp")
public class MaytapiWhatsAppWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaytapiWhatsAppWebhookController.class);

    private final LeadConversationService leadConversationService;

    @Value("${whatsapp.maytapi.api.access.token}")
    private String WHATS_MAYTAPI_APP_ACCESS_TOKEN;

    public MaytapiWhatsAppWebhookController(LeadConversationService leadConversationService) {
        this.leadConversationService = leadConversationService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody MessagePayload payload,        // TODO here we might need to have just a string, cause i think we get here all kinds of payloads..and not all of them are "MessagePayload"
//            @RequestBody String rawPayload,
            @RequestParam("token") String apiToken) {

        // --- TEMPORARY DEBUGGING ---
//        LOGGER.info("------- Received Token from Header: '" + apiToken + "'");
//        LOGGER.info("------- Expected Token from Properties: '" + WHATS_MAYTAPI_APP_ACCESS_TOKEN + "'");

        // --- SECURITY CHECK ---
        // Verify that the token received in the header matches our expected token.
        if (apiToken == null || !apiToken.equals(WHATS_MAYTAPI_APP_ACCESS_TOKEN)) {
            LOGGER.warn("Unauthorized webhook attempt: Invalid or missing token.");
            // Reject the request with a 401 Unauthorized status.
            return new ResponseEntity<>("Invalid authentication token", HttpStatus.UNAUTHORIZED);
        }

        // If the token is valid, proceed with processing the message.
        LOGGER.info("--- New Authenticated Webhook Received ---");

        LOGGER.info("==================");
        LOGGER.info(payload.toString());
        LOGGER.info("==================");

        if (payload != null && payload.getMessage() != null) {
            String sender = payload.getUser().getPhone();
            String messageText = payload.getMessage().getText();
            String messageId = payload.getMessage().getId();
            var isFromMe = payload.getMessage().isFromMe();

            LOGGER.info("From: " + sender);
            LOGGER.info("Message: " + messageText);

            if (!isFromMe) {
                leadConversationService.saveConversationTextMessage(sender, messageId, messageText, MessageType.RECEIVED);

                // TODO how to mark the message as read.. ?!!?
            }

        } else {
            LOGGER.info("Received a webhook event without a message body.");
        }

        // Always return a 200 OK response to Maytapi to acknowledge receipt.
        return ResponseEntity.ok("Webhook received successfully.");
    }
}

// todo move to different class
/**
 * Root object that matches the JSON you provided.
 */
class MessagePayload {

    /* ----- Primitive / simple fields -------------------------------------- */

    @JsonProperty("product_id")
    @JsonAlias("productId")          // handles both product_id and productId keys
    private String productId;

    @JsonProperty("phone_id")
    @JsonAlias("phoneId")            // handles both phone_id and phoneId keys
    private Integer phoneId;

    private String conversation;     // "40726276057@c.us"
    @JsonProperty("conversation_name")
    private String conversationName;

    private String receiver;         // "40771734054"
    private long timestamp;          // 1757603018
    private String type;             // "message"
    private String reply;            // https://api.maytapi.com/...

    /* ----- Nested objects ----------------------------------------------- */

    private Message message;
    private User user;

    /* ----- Getters & setters -------------------------------------------- */

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getPhoneId() {
        return phoneId;
    }

    public void setPhoneId(Integer phoneId) {
        this.phoneId = phoneId;
    }

    public String getConversation() {
        return conversation;
    }

    public void setConversation(String conversation) {
        this.conversation = conversation;
    }

    public String getConversationName() {
        return conversationName;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "MessagePayload{" +
                "productId='" + productId + '\'' +
                ", phoneId=" + phoneId +
                ", conversation='" + conversation + '\'' +
                ", conversationName='" + conversationName + '\'' +
                ", receiver='" + receiver + '\'' +
                ", timestamp=" + timestamp +
                ", type='" + type + '\'' +
                ", reply='" + reply + '\'' +
                ", message=" + message +
                ", user=" + user +
                '}';
    }

    /* ----- Inner helper classes ------------------------------------------ */

    public static class Message {
        private String type;          // "text"
        private String text;          // "hopa 3"
        private String id;            // "false_40726276057@c.us_ACF71EAFA7A288ED7D7D9E4ED688AF77"
        @JsonProperty("_serialized")
        private String serialized;    // same as id in the sample
        @JsonProperty("fromMe")
        private boolean fromMe;       // false

        // Getters / Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSerialized() {
            return serialized;
        }

        public void setSerialized(String serialized) {
            this.serialized = serialized;
        }

        public boolean isFromMe() {
            return fromMe;
        }

        public void setFromMe(boolean fromMe) {
            this.fromMe = fromMe;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "type='" + type + '\'' +
                    ", text='" + text + '\'' +
                    ", id='" + id + '\'' +
                    ", serialized='" + serialized + '\'' +
                    ", fromMe=" + fromMe +
                    '}';
        }
    }

    public static class User {
        private String id;            // "40726276057@c.us"
        private String name;          // "Dan"
        private String phone;         // "40726276057"
        private String image;         // URL to profile picture

        // Getters / Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", phone='" + phone + '\'' +
                    ", image='" + image + '\'' +
                    '}';
        }
    }
}