package com.ruby.ruby.service;

import com.ruby.ruby.command.CalculatorCommand;
import com.ruby.ruby.command.DateTimeCommand;
import com.ruby.ruby.command.FunCommand;
import com.ruby.ruby.command.GoodbyeCommand;
import com.ruby.ruby.command.GreetingCommand;
import com.ruby.ruby.command.IdentityCommand;
import com.ruby.ruby.command.MemoryCommand;
import com.ruby.ruby.command.NameCommand;
import com.ruby.ruby.command.NotesCommand;
import com.ruby.ruby.command.Reply;
import com.ruby.ruby.command.SmallTalkCommand;
import com.ruby.ruby.command.StopCommand;
import com.ruby.ruby.command.WebsiteCommand;
import com.ruby.ruby.memory.ConversationMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RubyServiceTest {

    /** Same order as the @Order values on the command beans. */
    private final RubyService service = new RubyService(List.of(
            new NameCommand(),
            new StopCommand(),
            new WebsiteCommand(),
            new CalculatorCommand(),
            new NotesCommand(),
            new MemoryCommand(),
            new IdentityCommand("Tester"),
            new FunCommand(),
            new DateTimeCommand(),
            new SmallTalkCommand(),
            new GoodbyeCommand(),
            new GreetingCommand()));

    private ConversationMemory memory;

    /** Most tests run as a user who has already told RUBY their name. */
    @BeforeEach
    void setUp() {
        memory = new ConversationMemory();
        memory.setUserName("Sam");
    }

    private String ask(String text) {
        return service.process(text, memory).text();
    }

    @Test
    void greetsWithConfiguredName() {
        assertEquals("Hello Sam. How can I assist you today?", ask("Hello"));
        assertEquals("Hello Sam. How can I assist you today?", ask("hey Ruby"));
        assertEquals("Hello Sam. How can I assist you today?", ask("Ruby"));
        assertEquals("I was created by Tester.", ask("who made you"));
        assertEquals("I was created by Tester.", ask("who is your creator"));
        assertEquals("Your name is Sam.", ask("what is my name"));
        assertEquals("My name is RUBY.", ask("what's your name"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"what is this", "show my history", "weather in delhi is it hot",
            "when is my birthday", "one more time"})
    void doesNotMatchKeywordsInsideOtherWordsOrPhrases(String text) {
        String answer = ask(text);
        assertFalse(answer.startsWith("Hello"), answer);
        assertFalse(answer.startsWith("Today is"), answer);
        assertFalse(answer.startsWith("The current time"), answer);
    }

    /** Each phrase must reach the right command, wake words and politeness included. */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "Ruby, what time is it          | The current time is",
            "hi, what time is it            | The current time is",
            "what's the time please         | The current time is",
            "what is today's date           | Today's date is",
            "what day is today              | Today is",
            "show me the date and time      | It is",
            "what's the weather today       | I cannot check the weather",
            "what is 5 plus 3               | The answer is 8.",
            "calculate what is 5 plus 3     | The answer is 8.",
            "calculate five plus three      | The answer is 8.",
            "calculate twenty five times 4  | The answer is 100.",
            "calculate 2 to the power of 10 | The answer is 1024.",
            "calculate 50% of 200           | The answer is 100.",
            "calculate 6 into 7             | The answer is 42.",
            "what is 9 squared              | The answer is 81.",
            "Ruby remember to call mom      | Got it. I will remember: call mom.",
            "please remember my code is 42  | Got it. I will remember: my code is 42.",
            "can you remind me to buy milk  | Got it. I will remember: buy milk.",
            "what are my notes              | You have no saved notes.",
            "help                           | I can tell you",
            "what can you do                | I can tell you",
            "thank you                      | You're welcome.",
            "thanks Ruby                    | You're welcome.",
            "can you hear me                | Yes, I can hear you",
            "how's it going                 | I am functioning perfectly",
            "roll the dice                  | You rolled",
            "toss a coin                    | ",
            "give me a random number        | Your random number is",
            "tell me a joke one more time   | ",
            "good night                     | Good night Sam.",
            "have a nice day                | Thank you. You too.",
    })
    void understandsNaturalPhrasing(String text, String expectedStart) {
        String answer = ask(text.trim());
        assertTrue(answer.startsWith(expectedStart == null ? "" : expectedStart.trim()),
                "'" + text.trim() + "' -> " + answer);
        assertFalse(answer.equals(RubyService.FALLBACK), "'" + text.trim() + "' was not understood");
    }

    // ---- Asking for and remembering the user's name ----

    private ConversationMemory newUser() {
        memory = new ConversationMemory();
        return memory;
    }

    @Test
    void introAsksForNameThenAcceptsBareAnswer() {
        newUser();
        assertEquals("Hello, I am RUBY, your voice assistant. What is your name?",
                service.introduce(memory).text());
        assertEquals("Nice to meet you, Priya. How can I help you today?", ask("priya"));
        assertEquals("Hello Priya. How can I assist you today?", ask("hello"));
        assertEquals("Your name is Priya.", ask("what is my name"));
        assertTrue(ask("goodbye").startsWith("Goodbye Priya."));
        assertEquals("Welcome back, Priya. How can I help you?", service.introduce(memory).text());
    }

    @ParameterizedTest
    @CsvSource({
            "my name is priya sharma, Priya Sharma",
            "I'm Rahul., Rahul",
            "Ruby call me Ash, Ash",
            "it's Mary Jane, Mary Jane",
            "this is McKenzie, McKenzie",
    })
    void acceptsNameAnswers(String answer, String name) {
        newUser();
        service.introduce(memory);
        ask(answer);
        assertEquals(java.util.Optional.of(name), memory.userName());
    }

    @Test
    void greetingWithoutNameAsksForIt() {
        newUser();
        assertEquals("Hello! I am RUBY. What is your name?", ask("hi"));
        ask("Neha");
        assertEquals("Hello Neha. How can I assist you today?", ask("hey"));
    }

    @Test
    void commandsStillWorkWhileWaitingForName() {
        newUser();
        service.introduce(memory);
        assertTrue(ask("what time is it").startsWith("The current time is"));
        assertTrue(memory.userName().isEmpty());
        assertTrue(memory.awaitingName(), "still waiting for the name");
        ask("Arjun");
        assertEquals(java.util.Optional.of("Arjun"), memory.userName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"I am fine", "not sure", "yes", "ok", "what", "12345", "blah blah"})
    void rejectsAnswersThatAreNotNames(String answer) {
        newUser();
        service.introduce(memory);
        ask(answer);
        assertTrue(memory.userName().isEmpty(), answer + " was taken as a name");
    }

    @Test
    void introductionPhrasesOnlyCountRightAfterAsking() {
        // Name already known and RUBY did not ask: "I'm hungry" must not rename the user.
        ask("I'm hungry");
        assertEquals(java.util.Optional.of("Sam"), memory.userName());
        // But "call me" always works.
        assertEquals("Nice to meet you, Sammy. How can I help you today?", ask("call me Sammy"));
    }

    @Test
    void noNameMeansNoNameInReplies() {
        newUser();
        assertEquals("I do not know your name yet. What should I call you?", ask("what is my name"));
        assertEquals("Goodbye. Have a productive day.", ask("bye"));
        assertEquals("I was created by Tester.", ask("who made you"));
    }

    @Test
    void jokeIsNotTheTime() {
        assertFalse(ask("tell me a joke one more time").startsWith("The current time"));
    }

    @Test
    void calculatesWithPrecedence() {
        assertEquals("The answer is 14.", ask("calculate 2 + 3 * 4"));
        assertEquals("The answer is 5.", ask("calculate 10 minus 3 minus 2"));
        assertEquals("Division by zero is not allowed.", ask("calculate 5 divided by 0"));
        assertEquals("Sorry, I could not understand that calculation.", ask("calculate banana"));
    }

    @Test
    void questionsThatAreNotMathAreLeftAlone() {
        assertEquals("My name is RUBY.", ask("what is your name"));
        assertEquals(RubyService.FALLBACK, ask("what is 5"));
    }

    @Test
    void notesKeepOriginalText() {
        ask("remember buy 2 plus size shirts");
        ask("Remember that Mom's birthday is Friday");
        assertEquals("You have 2 notes: 1. buy 2 plus size shirts. 2. Mom's birthday is Friday.",
                ask("read my notes"));
        assertEquals("All notes have been cleared.", ask("delete my notes"));
        assertEquals("You have no saved notes.", ask("show notes"));
    }

    @Test
    void remembersPreviousCommand() {
        assertEquals("You have not asked me anything before this.", ask("what did i ask before"));
        ask("tell me a joke");
        assertEquals("Your previous command was: tell me a joke", ask("what was my last command"));
        ask("Ruby, can you hear me");
        assertEquals("Your previous command was: Ruby, can you hear me", ask("what did i say before"));
    }

    @ParameterizedTest
    @CsvSource({
            "open YouTube, https://www.youtube.com",
            "open you tube, https://www.youtube.com",
            "Ruby open chat GPT, https://chatgpt.com",
            "open ChatGPT, https://chatgpt.com",
            "go to git hub, https://github.com",
            "please open google.com, https://www.google.com",
    })
    void opensWebsites(String text, String url) {
        assertEquals(url, service.process(text, memory).url());
    }

    @Test
    void plainRepliesHaveNoUrlAndKeepListening() {
        Reply reply = service.process("hello", memory);
        assertNull(reply.url());
        assertFalse(reply.stopListening());
    }

    @ParameterizedTest
    @ValueSource(strings = {"stop", "Stop.", "stop listening", "Ruby stop", "goodbye", "bye Ruby", "never mind"})
    void stopsListening(String text) {
        assertTrue(service.process(text, memory).stopListening(), text);
    }

    @Test
    void historyIsCapped() {
        for (int i = 0; i < ConversationMemory.MAX_HISTORY + 10; i++) {
            ask("hello");
        }
        assertEquals(ConversationMemory.MAX_HISTORY, memory.commandCount());
    }

    @Test
    void diceStaysInRange() {
        for (int i = 0; i < 100; i++) {
            int roll = Integer.parseInt(ask("roll a dice").replaceAll("\\D", ""));
            assertTrue(roll >= 1 && roll <= 6, "rolled " + roll);
        }
    }
}
