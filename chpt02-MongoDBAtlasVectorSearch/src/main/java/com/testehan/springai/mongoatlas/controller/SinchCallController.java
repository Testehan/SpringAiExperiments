package com.testehan.springai.mongoatlas.controller;

import com.testehan.springai.mongoatlas.service.SinchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SinchCallController {

    @Autowired
    private SinchService sinchService;

    @GetMapping("/callBySinch")
    public void getCalledBySinch(
            @RequestParam(value = "message", defaultValue = "Buna Dan, acesta e un call de la Sinch. Felicitari ! Ai castigat 100000000 euro.")
            String message) throws Exception {

        sinchService.sendMessage(message);
    }


}
