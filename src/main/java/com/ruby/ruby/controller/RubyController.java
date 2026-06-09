package com.ruby.ruby.controller;

import com.ruby.ruby.dto.ChatRequest;
import com.ruby.ruby.dto.ChatResponse;
import com.ruby.ruby.service.RubyService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RubyController {

    private final RubyService rubyService;

    public RubyController(RubyService rubyService) {
        this.rubyService = rubyService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(
            @RequestBody ChatRequest request) {

        String result =
                rubyService.processMessage(
                        request.getMessage());

        String action = null;

        if(result.startsWith("OPEN_")) {

            action = result;

            result =
                    "Opening requested website.";
        }

        return new ChatResponse(
                result,
                action
        );
    }

    @GetMapping("/hello")
    public String hello() {
        return "RUBY is online";
    }
}