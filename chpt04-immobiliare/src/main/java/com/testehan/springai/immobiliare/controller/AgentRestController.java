package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.service.LeadService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentRestController {

    private final LeadService leadService;

    public AgentRestController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping("/leads/url")
    public void getJsonContainingLeadURLs(HttpServletResponse response) {
        leadService.downloadJsonContainingLeadURLs(response);
    }
}
