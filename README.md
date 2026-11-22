# RUBY AI Assistant

RUBY is a voice-controlled AI assistant built using Spring Boot, Java, HTML, CSS, and JavaScript.

The project combines speech recognition, speech synthesis, real-time UI updates, command processing, and an interactive dashboard to create a modern AI assistant experience directly in the browser.

---

# Features

- Voice recognition with live subtitles, and natural female voice responses (with a voice picker)
- Greets you by name and time of day; remembers your name and notes on your device
- Live weather for any city (Open-Meteo, no API key)
- "Who is…" / "Tell me about…" answers from Wikipedia, with photo cards
- Timers and spoken reminders
- Voice search on YouTube and Google, and a website launcher
- Calculator with spoken maths ("what is 25 times 4", "2 to the power of 10", "50% of 200")
- Holographic HUD: voice-reactive core, result cards, interface sounds, suggestion chips
- Holographic globe with live coordinates: turns to any city you ask about; LOCATE shows your real position
- Cinematic dashboard entrance and 3D parallax tilt that follows the mouse
- Real readouts: server latency, device battery, network, CPU cores, live weather
- Jokes, motivation, dice, coin flip, random numbers
- Boot sequence, animated circuit background, responsive layout for phones
- Installable as an app (PWA)

---

# Technology Stack

## Backend

- Java
- Spring Boot
- Maven
- REST APIs

## Frontend

- HTML5
- CSS3
- JavaScript

## Browser APIs

- Web Speech API (recognition and synthesis)
- Web Audio API (interface sounds, microphone level)
- Canvas API
- Battery Status and Network Information APIs (where supported)
- Service Worker and Web App Manifest

## Online services (free, no API keys)

- Open-Meteo for weather
- Wikipedia for knowledge answers

---

# Available Commands

You can start any command with "Ruby" or "please", and speak naturally.

## Weather

```text
what's the weather
weather in Mumbai
is it raining in London
```

---

## Questions (Wikipedia)

```text
who is APJ Abdul Kalam
tell me about the Taj Mahal
what is photosynthesis
```

---

## Timers and Reminders

```text
set a timer for 5 minutes
remind me in 2 minutes to drink water
cancel all timers
```

---

## Search

```text
search YouTube for lofi music
play Arijit Singh songs
google Spring Boot tutorial
```

---

## Your Name

```text
my name is Priya
call me Priya
what is my name
```

---

## Greetings

```text
hello
hi
hey
```

---

## Time

```text
time
what time is it
```

---

## Date

```text
date
what is today's date
```

---

## Day

```text
day
what day is it
```

---

## Assistant Information

```text
who are you
your name
who created you
who made you
how are you
```

---

## System Commands

```text
system status
what can you do
```

---

## Motivation

```text
motivate me
```

---

## Jokes

```text
joke
tell me a joke
```

---

## Calculator

### Addition

```text
calculate 5 plus 5
```

### Subtraction

```text
calculate 20 minus 10
```

### Multiplication

```text
calculate 8 times 4
```

### Division

```text
calculate 100 divided by 5
```

---

## Notes Memory

### Save Note

```text
remember learn spring boot
```

### View Notes

```text
show notes
```

### Clear Notes

```text
clear notes
```

---

## Memory Commands

### Previous Command

```text
what did i ask before
```

### Memory Status

```text
memory status
```

---

## Dice

```text
roll dice
roll a dice
```

---

## Coin Flip

```text
flip coin
flip a coin
```

---

## Random Number

```text
random number
```

---

## Website Launcher

```text
open google
open youtube
open github
open chatgpt
```

---

## Exit Commands

These also switch voice mode off.

```text
stop
bye
goodbye
good night
```

---

# Project Structure

```text
src/main/java/com/ruby/ruby
├── command        one class per command (weather, timers, notes, ...)
├── config         clock and HTTP client setup
├── controller     REST API (/api/chat, /api/intro, /api/weather)
├── dto            request and response objects
├── integration    Open-Meteo and Wikipedia clients
├── memory         per-session conversation memory
└── service        routes each message to the right command

src/main/resources/static
├── index.html     dashboard
├── style.css      HUD styling
├── app.js         conversation, subtitles, voice-reactive core
├── hud.js         boot screen, background, live readouts
├── voice.js       natural voice selection
├── widgets.js     result cards and timers
├── globe.js       holographic globe (canvas, no libraries)
├── globe-data.js  land dots from Natural Earth (public domain)
├── sfx.js         interface sounds
├── sw.js          service worker (installable app)
└── manifest.webmanifest
```

See [DEMO.md](DEMO.md) for a presentation script.

---

# Running Locally

Requires Java 17 or newer.

Clone the repository:

```bash
git clone https://github.com/yashcodesai/ruby-ai-assistant.git
```

Navigate to the project:

```bash
cd ruby-ai-assistant
```

Run the application:

```bash
./mvnw spring-boot:run
```

or

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080
```

Voice input works in Chrome and Edge. In other browsers, type commands into the text box.

Run the tests:

```bash
./mvnw test
```

When the dashboard loads, RUBY asks for your name and uses it for the rest of the session. Say "call me ..." to change it.

## Getting a natural voice

RUBY uses the most natural female voice your browser offers, and you can pick another from the **VOICE** menu (★ marks natural-sounding voices):

- **Microsoft Edge:** has free neural voices such as Aria, Jenny, Sonia and Neerja that sound close to Alexa.
- **Mac (Safari/Chrome):** open System Settings → Accessibility → Spoken Content → System Voice → Manage Voices, and download an English voice marked **Premium** (for example Ava or Zoe).
- **Chrome:** "Google UK English Female" is used automatically.

## Settings

In `src/main/resources/application.properties`:

- `ruby.weather.default-city`: city used for "what's the weather" and the dashboard panel (default: New Delhi)
- `ruby.creator-name`: credit yourself in answers to "who created you"

---

# Future Enhancements

- Android app (APK)
- AI answers for open-ended questions
- Advanced Memory
- Voice Profiles
- Theme Switching
- Desktop Automation
- Smart Task Management

---

# Author

**Yash Saxena**

Software Developer | Java Developer | AI Enthusiast

GitHub:
https://github.com/yashcodesai

---

# License

This project is available for learning, experimentation, and personal use.
