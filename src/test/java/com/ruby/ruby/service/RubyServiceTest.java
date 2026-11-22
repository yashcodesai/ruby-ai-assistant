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
import com.ruby.ruby.command.KnowledgeCommand;
import com.ruby.ruby.command.SearchCommand;
import com.ruby.ruby.command.TimerCommand;
import com.ruby.ruby.command.WeatherCommand;
import com.ruby.ruby.integration.Article;
import com.ruby.ruby.integration.ServiceUnavailableException;
import com.ruby.ruby.integration.WeatherReport;
import com.ruby.ruby.memory.ConversationMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RubyServiceTest {

    /** 2026-11-22 08:05 UTC = 1:35 PM in India. */
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-22T08:05:00Z"), ZoneOffset.UTC);

    /** Fake online services: tests never touch the network. */
    static final WeatherReport DELHI = new WeatherReport("New Delhi", "India", 28.62, 77.21, 24.2, 24.6, 47, 2.1, 0, true);
    static boolean offline = false;

    static Optional<WeatherReport> fakeWeather(String place) {
        if (offline) throw new ServiceUnavailableException("offline", null);
        return place.equalsIgnoreCase("new delhi") ? Optional.of(DELHI)
                : place.equalsIgnoreCase("mumbai")
                ? Optional.of(new WeatherReport("Mumbai", "India", 19.08, 72.88, 31.6, 34.0, 60, 9.0, 2, true))
                : Optional.empty();
    }

    static Optional<Article> fakeWikipedia(String topic) {
        if (offline) throw new ServiceUnavailableException("offline", null);
        return topic.toLowerCase().contains("kalam")
                ? Optional.of(new Article("A. P. J. Abdul Kalam",
                "Avul Pakir Jainulabdeen Abdul Kalam was an Indian aerospace engineer. "
                        + "He served as president of India from 2002 to 2007. He was born in Rameswaram.",
                "https://example.org/kalam.jpg", "https://en.wikipedia.org/wiki/A._P._J._Abdul_Kalam"))
                : Optional.empty();
    }

    /** Same order as the @Order values on the command beans. */
    private final RubyService service = new RubyService(List.of(
            new NameCommand(),
            new StopCommand(),
            new WebsiteCommand(),
            new SearchCommand(),
            new CalculatorCommand(),
            new TimerCommand(),
            new NotesCommand(),
            new MemoryCommand(),
            new IdentityCommand("Tester"),
            new FunCommand(),
            new WeatherCommand(RubyServiceTest::fakeWeather, "New Delhi"),
            new DateTimeCommand(CLOCK),
            new SmallTalkCommand(),
            new GoodbyeCommand(),
            new KnowledgeCommand(RubyServiceTest::fakeWikipedia),
            new GreetingCommand()), CLOCK);

    private ConversationMemory memory;

    /** Most tests run as a user who has already told RUBY their name. */
    @BeforeEach
    void setUp() {
        memory = new ConversationMemory();
        memory.setUserName("Sam");
        memory.setZone("Asia/Kolkata");
        offline = false;
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
            "what's the weather today       | In New Delhi it is 24 degrees, clear sky.",
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
            "help                           | I can check the weather",
            "what can you do                | I can check the weather",
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
        memory.setZone("Asia/Kolkata");
        return memory;
    }

    @Test
    void introAsksForNameThenAcceptsBareAnswer() {
        newUser();
        assertEquals("Good afternoon. I am RUBY, your personal assistant. All systems are online. "
                + "May I know your name?", service.introduce(memory).text());
        assertEquals("Nice to meet you, Priya. How can I help you today?", ask("priya"));
        assertEquals("Hello Priya. How can I assist you today?", ask("hello"));
        assertEquals("Your name is Priya.", ask("what is my name"));
        assertTrue(ask("goodbye").startsWith("Goodbye Priya."));
        assertEquals("Good afternoon, Priya. All systems are online. How can I help you?",
                service.introduce(memory).text());
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

    // ---- Time in the user's own timezone ----

    @Test
    void timeAndGreetingUseUsersTimezone() {
        assertEquals("The current time is 1:35 PM.", ask("what time is it"));
        memory.setZone("America/New_York");
        assertEquals("The current time is 3:05 AM.", ask("what time is it"));
        assertTrue(service.introduce(memory).text().startsWith("Good evening, Sam."));
        memory.setZone("Not/AZone");
        assertEquals("The current time is 3:05 AM.", ask("what time is it"), "invalid zone is ignored");
    }

    // ---- Weather ----

    @Test
    void weatherForDefaultCityWithCard() {
        Reply reply = service.process("what's the weather", memory);
        assertEquals("In New Delhi it is 24 degrees, clear sky. It feels like 25 degrees. Humidity is 47 percent.",
                reply.text());
        assertEquals("weather", reply.card().type());
        assertEquals("New Delhi, India", reply.card().title());
        assertEquals("24°C", reply.card().facts().get(0).value());
        assertEquals(28.62, reply.card().location().latitude(), 1e-9, "globe marker position");
        assertEquals("New Delhi", reply.card().location().label());
    }

    @ParameterizedTest
    @ValueSource(strings = {"weather in Mumbai", "what's the temperature in mumbai today",
            "Ruby, is it raining in Mumbai?", "how hot is it in mumbai right now"})
    void weatherForNamedCity(String text) {
        assertTrue(ask(text).startsWith("In Mumbai it is 32 degrees"), ask(text));
    }

    @Test
    void weatherErrors() {
        assertEquals("I could not find a place called Atlantis City.", ask("weather in atlantis city"));
        offline = true;
        assertTrue(ask("what's the weather").startsWith("I could not reach the weather service"));
    }

    // ---- Wikipedia ----

    @Test
    void answersFromWikipediaWithCard() {
        Reply reply = service.process("who is APJ Abdul Kalam", memory);
        assertEquals("Avul Pakir Jainulabdeen Abdul Kalam was an Indian aerospace engineer. "
                + "He served as president of India from 2002 to 2007.", reply.text(), "first two sentences");
        assertEquals("knowledge", reply.card().type());
        assertEquals("https://example.org/kalam.jpg", reply.card().imageUrl());
        assertTrue(ask("tell me about Abdul Kalam").startsWith("Avul"));
    }

    @Test
    void wikipediaErrors() {
        assertEquals("I could not find anything about Qwzxv. Try saying it a different way.", ask("who is Qwzxv"));
        offline = true;
        assertTrue(ask("who is Abdul Kalam").startsWith("I could not reach Wikipedia"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"what is my name", "what is your name", "what is 5 plus 3", "what time is it",
            "what is today's date", "what is this", "what's up", "who are you", "what is the weather"})
    void otherQuestionsNeverReachWikipedia(String text) {
        offline = true; // any Wikipedia call would answer "could not reach Wikipedia"
        assertFalse(ask(text).contains("Wikipedia"), text + " -> " + ask(text));
    }

    // ---- Search ----

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "search YouTube for lofi music      | https://www.youtube.com/results?search_query=lofi+music",
            "search lofi music on YouTube       | https://www.youtube.com/results?search_query=lofi+music",
            "Ruby play arijit singh songs       | https://www.youtube.com/results?search_query=arijit+singh+songs",
            "google spring boot tutorial        | https://www.google.com/search?q=spring+boot+tutorial",
            "search for best pizza near me      | https://www.google.com/search?q=best+pizza+near+me",
            "search google for c++ & java       | https://www.google.com/search?q=c%2B%2B+%26+java",
    })
    void searchesTheWeb(String text, String url) {
        assertEquals(url.trim(), service.process(text.trim(), memory).url());
    }

    // ---- Timers and reminders ----

    @Test
    void setsTimers() {
        Reply reply = service.process("set a timer for 5 minutes", memory);
        assertEquals("Timer set for 5 minutes.", reply.text());
        assertEquals(300, reply.timer().seconds());
        assertEquals("5 minute timer", reply.timer().label());
        assertEquals("Sam, your 5 minute timer is done.", reply.timer().doneText());

        assertEquals(90, service.process("timer for one minute and 30 seconds", memory).timer().seconds());
        assertEquals(1800, service.process("set a timer for half an hour", memory).timer().seconds());
        assertEquals(5400, service.process("countdown 1 and a half hours", memory).timer().seconds());
    }

    @Test
    void setsReminders() {
        Reply reply = service.process("remind me in 2 minutes to call my mom", memory);
        assertEquals("Okay. I will remind you to call your mom in 2 minutes.", reply.text());
        assertEquals(120, reply.timer().seconds());
        assertEquals("Call your mom", reply.timer().label());
        assertEquals("Sam, this is your reminder to call your mom.", reply.timer().doneText());

        Reply after = service.process("Ruby remind me to drink water in half an hour", memory);
        assertEquals("Okay. I will remind you to drink water in 30 minutes.", after.text());
    }

    @Test
    void reminderWithoutTimeIsANote() {
        Reply reply = service.process("remind me to buy eggs", memory);
        assertNull(reply.timer());
        assertEquals("Got it. I will remember: buy eggs.", reply.text());
    }

    @Test
    void cancelsTimersAndExplainsAlarms() {
        assertTrue(service.process("cancel all timers", memory).cancelTimers());
        assertTrue(service.process("stop the reminder", memory).cancelTimers());
        assertTrue(ask("set an alarm for 7 am").startsWith("I can set timers for a length of time"));
        assertNull(service.process("set a timer for 2 days", memory).timer(), "over 24 hours is refused");
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
