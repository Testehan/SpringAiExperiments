package com.testehan.springai.immobiliare.model;

import java.io.InputStream;

public record ApartmentImage(String name, String contentType, InputStream data) {
}
