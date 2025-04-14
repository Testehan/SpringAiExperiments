package com.testehan.springai.immobiliare.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document(collection = "configurations")
public class AppConfigurations {
    @Id
    private String id = "configurations"; // Ensure a single document

    private boolean scheduled_task_resetSearchesAvailable = true;
    private boolean scheduled_task_deactivatedOldListings = false;
    private boolean scheduled_task_sendReactivationEmail = false;
    private boolean scheduled_task_sendReactivationSMS = false;
    private boolean email_sendWelcomeEmail = false;
    private boolean email_sendListingAddedEmail = false;

    private String lastUpdateDateTime;
}
