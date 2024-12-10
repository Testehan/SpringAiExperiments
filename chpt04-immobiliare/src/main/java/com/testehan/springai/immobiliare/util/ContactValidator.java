package com.testehan.springai.immobiliare.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.Optional;

public class ContactValidator {

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

    public static boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
    }

}
