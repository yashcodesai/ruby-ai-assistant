package com.ruby.ruby.dto;

import com.ruby.ruby.command.Card;
import com.ruby.ruby.command.Reply;
import com.ruby.ruby.command.TimerSpec;
import com.ruby.ruby.memory.ConversationMemory;

import java.util.List;

/**
 * @param response      text RUBY says back
 * @param url           website the browser should open, or null
 * @param stopListening true if the browser should switch voice mode off
 * @param card          rich result to display, or null
 * @param timer         countdown to start, or null
 * @param cancelTimers  true if the browser should cancel its timers
 * @param userName      the user's name, or null if not known yet (saved on the device)
 * @param notes         the user's notes (saved on the device)
 */
public record ChatResponse(String response, String url, boolean stopListening,
                           Card card, TimerSpec timer, boolean cancelTimers,
                           String userName, List<String> notes) {

    public static ChatResponse of(Reply reply, ConversationMemory memory) {
        return new ChatResponse(reply.text(), reply.url(), reply.stopListening(),
                reply.card(), reply.timer(), reply.cancelTimers(),
                memory.userName().orElse(null), memory.notes());
    }

    public static ChatResponse error(String response) {
        return new ChatResponse(response, null, false, null, null, false, null, null);
    }
}
