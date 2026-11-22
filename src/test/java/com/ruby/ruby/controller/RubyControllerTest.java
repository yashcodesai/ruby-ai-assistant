package com.ruby.ruby.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RubyControllerTest {

    @Autowired
    private MockMvc mvc;

    private org.springframework.test.web.servlet.ResultActions chat(String json, MockHttpSession session)
            throws Exception {
        return mvc.perform(post("/api/chat").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void answersAMessage() throws Exception {
        chat("{\"message\":\"open github\"}", new MockHttpSession())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Opening GitHub."))
                .andExpect(jsonPath("$.url").value("https://github.com"))
                .andExpect(jsonPath("$.stopListening").value(false));
    }

    @Test
    void goodbyeTellsBrowserToStopListening() throws Exception {
        chat("{\"message\":\"goodbye\"}", new MockHttpSession())
                .andExpect(jsonPath("$.stopListening").value(true));
    }

    @Test
    void nameIsAskedThenRememberedForTheSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/api/session").session(session))
                .andExpect(jsonPath("$.userName").doesNotExist());

        mvc.perform(post("/api/intro").session(session))
                .andExpect(jsonPath("$.response", containsString("May I know your name")))
                .andExpect(jsonPath("$.userName").doesNotExist());

        chat("{\"message\":\"my name is Priya\"}", session)
                .andExpect(jsonPath("$.userName").value("Priya"));

        chat("{\"message\":\"hello\"}", session)
                .andExpect(jsonPath("$.response").value("Hello Priya. How can I assist you today?"));

        mvc.perform(get("/api/session").session(session))
                .andExpect(jsonPath("$.userName").value("Priya"));

        // A different visitor is asked for their own name.
        chat("{\"message\":\"hello\"}", new MockHttpSession())
                .andExpect(jsonPath("$.response").value("Hello! I am RUBY. What is your name?"));
    }

    @Test
    void rejectsMissingOrBlankMessage() throws Exception {
        chat("{}", new MockHttpSession()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.response").exists());
        chat("{\"message\":\"   \"}", new MockHttpSession()).andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonDoesNotLeakStackTrace() throws Exception {
        chat("not json", new MockHttpSession())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void introRestoresNameAndNotesSavedOnDevice() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/intro").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"neha\",\"notes\":[\"buy milk\",\"  \"],"
                                + "\"timeZone\":\"Asia/Kolkata\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response", containsString(", Neha. All systems are online.")))
                .andExpect(jsonPath("$.userName").value("Neha"))
                .andExpect(jsonPath("$.notes.length()").value(1))
                .andExpect(jsonPath("$.notes[0]").value("buy milk"));

        chat("{\"message\":\"remember call dad\"}", session)
                .andExpect(jsonPath("$.notes.length()").value(2));
    }

    @Test
    void introIgnoresInvalidSavedName() throws Exception {
        mvc.perform(post("/api/intro").session(new MockHttpSession()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"<script>1</script>\"}"))
                .andExpect(jsonPath("$.userName").doesNotExist())
                .andExpect(jsonPath("$.response", containsString("May I know your name")));
    }

    @Test
    void timerReplyCarriesCountdown() throws Exception {
        chat("{\"message\":\"set a timer for 10 seconds\"}", new MockHttpSession())
                .andExpect(jsonPath("$.timer.seconds").value(10))
                .andExpect(jsonPath("$.timer.doneText").exists());
    }

    @Test
    void rejectsOverlongMessage() throws Exception {
        String longMessage = "a".repeat(RubyController.MAX_MESSAGE_LENGTH + 1);
        chat("{\"message\":\"" + longMessage + "\"}", new MockHttpSession())
                .andExpect(status().isBadRequest());
    }

    @Test
    void notesAreSeparatePerSession() throws Exception {
        MockHttpSession alice = new MockHttpSession();
        MockHttpSession bob = new MockHttpSession();

        chat("{\"message\":\"remember Alice's secret\"}", alice).andExpect(status().isOk());

        chat("{\"message\":\"show notes\"}", alice)
                .andExpect(jsonPath("$.response", containsString("Alice's secret")));
        chat("{\"message\":\"show notes\"}", bob)
                .andExpect(jsonPath("$.response").value("You have no saved notes."));
    }
}
