package com.testehan.springai.immobiliare.util;

import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class WhatsAppLeadAutomation {

    public static void main(String[] args) throws InterruptedException, IOException {

        List<String> phoneNumbers = readLeadPhones();
//        sendFirstMessagesToLeads(phoneNumbers);

//        readChatConversationsForPhoneNumbers(phoneNumbers);

        selenium_openChatWithUnreadMessages();
    }

    public static void readChatConversationsForPhoneNumbers(List<String> phoneNumbers) throws InterruptedException {
        ChromeOptions options = getChromeOptions();

        WebDriver driver = new ChromeDriver(options);
        driver.get("https://web.whatsapp.com");

        // Wait for WhatsApp to fully load
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@role='textbox']"))
        );

        for (String number : phoneNumbers) {

            selenium_openChatBySearchingForNumber(number, driver);

            selenium_printChatConversation(driver);
        }
    }

    private static void selenium_printChatConversation(WebDriver driver) {
        // Get all message bubbles
        List<WebElement> allMessages = driver.findElements(By.xpath("//div[@role='row']"));

        for (int i = 0; i < allMessages.size(); i++) {
            WebElement msg = allMessages.get(i);

            List<WebElement> messageInDivs = msg.findElements(By.className("message-in"));
            List<WebElement> messageOutDivs = msg.findElements(By.className("message-out"));

            if (!messageInDivs.isEmpty()) {
                try {
                    WebElement copyableText = msg.findElement(By.className("copyable-text"));
                    String text = copyableText.getText();
                    System.out.println("Lead: " + text);
                } catch (Exception e) {
                    System.out.println("Could not extract text: " + e.getMessage());
                }
            }

            if (!messageOutDivs.isEmpty()) {
                try {
                    WebElement copyableText = msg.findElement(By.className("copyable-text"));
                    String text = copyableText.getText();
                    System.out.println("CasaMia.ai: " + text);
                } catch (Exception e) {
                    System.out.println("Could not extract text: " + e.getMessage());
                }
            }
        }
    }

    private static void selenium_openChatBySearchingForNumber(String number, WebDriver driver) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));

        try {
            // Click on the search bar
            WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@role='textbox']")
            ));
            searchBox.click();

            // clean searchbox
            searchBox.sendKeys(Keys.COMMAND + "a");  // Select all text
            searchBox.sendKeys(Keys.DELETE);         // Delete it

            // Type phone number
            searchBox.sendKeys(number);
            Thread.sleep(2000); // Give time for search results to load

            // Click on the chat result
            WebElement chatResult = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@aria-label='Search results.'][@role='grid']//div[@tabindex='-1'][1]")
            ));
            chatResult.click();

            System.out.println("Opened chat for: " + number);

        } catch (Exception e) {
            System.out.println("Could not open chat for " + number + ": " + e.getMessage());
        }

        Thread.sleep(5000); // Give time to load chat
    }

    private static void selenium_openChatWithUnreadMessages() throws InterruptedException {
        ChromeOptions options = getChromeOptions();

        WebDriver driver = new ChromeDriver(options);
        driver.get("https://web.whatsapp.com");

        // Wait for WhatsApp to fully load
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@role='textbox']"))
        );

        WebElement unreadButton = driver.findElement(By.id("unread-filter"));
        unreadButton.click();

        // Wait for the Chat list container to be visible
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement chatList = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div[aria-label='Chat list'][role='grid']"))
        );

        // Get all child elements (individual chats) inside the chat list
        List<WebElement> chats = chatList.findElements(By.cssSelector("div[role='listitem']"));

        // Click each chat one by one
        for (WebElement chat : chats) {
            try {
                chat.click();
                Thread.sleep(1000); // Optional: wait for content to load before next click

                selenium_printChatConversation(driver);

            } catch (Exception e) {
                System.out.println("Failed to click a chat: " + e.getMessage());
            }
        }

    }

    private static void sendFirstMessagesToLeads(List<String> phoneNumbers) throws InterruptedException {

        List<String> messages = new ArrayList<>(List.of(
                "Buna! Am vazut anuntul ... voiam sa te intrebam daca e inca valabil ?",
                "Buna ziua! Anuntul legat de proprietatea de inchiriat e inca valabil ?",
                "Buna! Am vazut anuntul dvs..Voiam sa știu daca se mai poate inchiria.",
                "Buna! Anuntul de inchiriat e inca de actualitate?"
        ));

        ChromeOptions options = getChromeOptions();

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        // Open WhatsApp Web
        driver.get("https://web.whatsapp.com/");
//        System.out.println("Scan the QR code, then press Enter to continue...");
//        System.in.read();
        // Wait for the page to load and allow setting cookies
        Thread.sleep(3000);


      /*  for (String number : phoneNumbers) {
            String randomMessage = messages.get(new Random().nextInt(messages.size()));
            String encodedMsg = URLEncoder.encode(randomMessage, StandardCharsets.UTF_8);
            String url = "https://wa.me/" + number + "?text=" + encodedMsg;

            driver.get(url);
            Thread.sleep(4000);

            // Click "Continue to Chat" and then "Use WhatsApp Web"
            try {
                driver.findElement(By.linkText("Continue to Chat")).click();
                Thread.sleep(5000);
                driver.findElement(By.linkText("use WhatsApp Web")).click();
                Thread.sleep(5000);

                // Click send button
                WebElement sendButton = driver.findElement(By.xpath("//span[@data-icon='send']"));
                sendButton.click();
                System.out.println("Message sent to: " + number);
                Thread.sleep(getRandomMillis());
            } catch (Exception e) {
                System.out.println("Failed to send to " + number + ": " + e.getMessage());
            }
        }*/

        for (String number : phoneNumbers) {
            String randomMessage = messages.get(new Random().nextInt(messages.size()));
            String encodedMsg = URLEncoder.encode(randomMessage, StandardCharsets.UTF_8);

            // Go directly to the WhatsApp chat page for that number
            String url = "https://web.whatsapp.com/send?phone=" + number + "&text=" + encodedMsg;
            driver.get(url);

            try {
                // Wait for message box to appear
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                WebElement sendButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[@data-icon='send']")));

                // Click the send button
                sendButton.click();
                System.out.println("Message sent to: " + number);
                Thread.sleep(getRandomMillis());

            } catch (Exception e) {
                System.out.println("Failed to send to " + number + ": " + e.getMessage());
            }
        }

        driver.quit();
    }

    @NotNull
    private static ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        // the content in this folder was copied from /Users/myuser/Library/Application Support/Google/Chrome
        options.addArguments("user-data-dir=/Users/danteshte/seleniumProfiles");
        options.addArguments("profile-directory=Default");
        return options;
    }

    private static int getRandomMillis(){
        int min = 30000;  // The minimum value (inclusive)
        int max = 300000;  // The maximum value (inclusive)

        Random random = new Random();
        return random.nextInt(max - min + 1) + min;

    }

    public static List<String> readLeadPhones() {
        String csvFile = "/Users/danteshte/JavaProjects/spring-ai-experiments/chpt04-immobiliare/src/test/resources/lead_phones.csv";
        List<String> phoneNumbers = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean firstLine = true; // To skip the header row

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false; // Skip the "Lead phone" header
                    continue;
                }

                // Assuming only one column and no commas in phone numbers
                // List of phone numbers in international format (no +)
                String phoneNumber = line.trim().replace("+",""); // Remove leading/trailing whitespace

                if (!phoneNumber.isEmpty()) {
                    phoneNumbers.add(phoneNumber);
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            e.printStackTrace(); // Print the stack trace for debugging
        }

        return phoneNumbers;
    }

}
