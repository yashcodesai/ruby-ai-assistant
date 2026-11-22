package com.ruby.ruby.controller;

import com.ruby.ruby.command.Reply;
import com.ruby.ruby.dto.ChatRequest;
import com.ruby.ruby.dto.ChatResponse;
import com.ruby.ruby.dto.SessionInfo;
import com.ruby.ruby.memory.ConversationMemory;
import com.ruby.ruby.service.RubyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api")
public class RubyController {

    static final int MAX_MESSAGE_LENGTH = 500;
    private static final String MEMORY_KEY = "ruby.memory";

    private final RubyService rubyService;

    public RubyController(RubyService rubyService) {
        this.rubyService = rubyService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody(required = false) ChatRequest request, HttpSession session) {

        String message = request == null ? null : request.message();
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ChatResponse.error("I did not hear anything. Please try again."));
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return ResponseEntity.badRequest()
                    .body(ChatResponse.error("That message is too long for me."));
        }

        ConversationMemory memory = memoryFor(session);
        Reply reply = rubyService.process(message, memory);
        return ResponseEntity.ok(toResponse(reply, memory));
    }

    /** RUBY's opening line after the boot screen; asks for the user's name if unknown. */
    @PostMapping("/intro")
    public ChatResponse intro(HttpSession session) {
        ConversationMemory memory = memoryFor(session);
        return toResponse(rubyService.introduce(memory), memory);
    }

    /** Lets the boot screen welcome a returning user by name. */
    @GetMapping("/session")
    public SessionInfo session(HttpSession session) {
        return new SessionInfo(memoryFor(session).userName().orElse(null));
    }

    @GetMapping("/hello")
    public String hello() {
        return "RUBY is online";
    }

    private static ChatResponse toResponse(Reply reply, ConversationMemory memory) {
        return new ChatResponse(reply.text(), reply.url(), reply.stopListening(),
                memory.userName().orElse(null));
    }

    /** Each browser session gets its own notes and history. */
    private static ConversationMemory memoryFor(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            ConversationMemory memory = (ConversationMemory) session.getAttribute(MEMORY_KEY);
            if (memory == null) {
                memory = new ConversationMemory();
                session.setAttribute(MEMORY_KEY, memory);
            }
            return memory;
        }
    }
}
