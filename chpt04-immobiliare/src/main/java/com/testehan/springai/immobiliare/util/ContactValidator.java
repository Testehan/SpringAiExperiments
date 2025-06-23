package com.testehan.springai.immobiliare.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ContactValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactValidator.class);

    public static boolean isValidPhoneNumber(String phoneNumber, String region) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, region);
            return phoneUtil.isValidNumber(parsedNumber);
        } catch (NumberParseException e) {
            return false;
        }
    }

    public static Optional<String> getPhoneNumberWithPrefix(String phoneNumber, String region) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNumber, region);

            // Format in E.164
            String e164Format = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
           return Optional.of(e164Format);

        }   catch (NumberParseException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> normalizePhoneNumber(String phoneNumber) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            // "RO" = Romania default region; if a phone prefix is not mentioned, this will be taken as default
            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNumber, "RO");

            // Always return E.164 (+40...) format
            return Optional.of(phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164));
        } catch (NumberParseException e) {
            LOGGER.warn("The provided phone number {} could not be normalized",phoneNumber);
            return Optional.empty();
        }
    }

    public static String internationalizePhoneNumber(String phoneNumber) {
        var optionalInternationalNumber = ContactValidator.normalizePhoneNumber(phoneNumber);
        return optionalInternationalNumber.isPresent() ? optionalInternationalNumber.get() : phoneNumber;
    }

    public static boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
    }

}
