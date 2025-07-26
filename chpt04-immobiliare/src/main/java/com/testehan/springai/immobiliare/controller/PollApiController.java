package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.Poll;
import com.testehan.springai.immobiliare.repository.PollRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/poll")
public class PollApiController {

    private final PollRepository pollRepository;

    public PollApiController(PollRepository pollRepository) {
        this.pollRepository = pollRepository;
    }

    @GetMapping("/{id}")
    public Poll getPoll(@PathVariable String id) {
        return pollRepository.findById(id).orElseThrow();
    }

    @PostMapping("/{id}/vote")
    public Poll vote(@PathVariable String id, @RequestParam String option) {
        Poll poll = pollRepository.findById(id).orElseThrow();
        poll.getOptions().merge(option, 1, Integer::sum);
        return pollRepository.save(poll);
    }
}
