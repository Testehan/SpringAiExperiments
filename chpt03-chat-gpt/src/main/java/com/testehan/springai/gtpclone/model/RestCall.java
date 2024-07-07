package com.testehan.springai.gtpclone.model;

import java.util.Map;

public record RestCall(String apiCall, Map<String, Object>Parameters) {
}
