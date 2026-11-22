# RUBY Presentation Script

A 5–7 minute live demo that shows every major feature.

## Before you present (10 minutes earlier)

1. Start the app: `./mvnw spring-boot:run`, then open **http://localhost:8080** in **Chrome** (or Edge for the most natural voices).
2. Allow the **microphone** when asked, and turn the laptop **volume up**.
3. Check the **internet** connection (needed for weather and Wikipedia). The top-right should say **RUBY ONLINE**.
4. Pick a voice in the **VOICE** menu (★ = natural). Selecting one plays a sample.
5. For a fresh "first meeting", clear the saved profile: open the browser console and run
   `localStorage.clear()`, then reload. RUBY will ask your name again.
6. Close other tabs that use the microphone.

## The demo

| # | Do / say | What the audience sees |
|---|---|---|
| 1 | Reload the page | Boot sequence with the Yash Industries logo, then the dashboard assembles itself |
| 2 | Point out the panels and move the mouse | Real readouts (latency, battery, network, weather); panels tilt in 3D; the globe shows your city's coordinates |
| 3 | Click **ACTIVATE** | Activation sound; RUBY greets you by time of day and asks your name |
| 4 | "My name is …" | "Nice to meet you…"; your name appears under SESSION |
| 5 | "What's the weather?" | Weather card with temperature, humidity and wind |
| 6 | "Weather in London" | The holographic globe turns to London and drops a beacon with its coordinates |
| 7 | "Who is APJ Abdul Kalam?" | Spoken answer plus a photo card from Wikipedia |
| 8 | "Set a timer for 30 seconds" | Countdown appears under ACTIVE TIMERS; keep talking, and RUBY chimes and speaks when it ends |
| 9 | "Remember that the presentation went well" | Note saved; NOTES counter goes up |
| 10 | "What is 25 times 4 plus 10?" | "The answer is 110." |
| 11 | "Tell me a joke" | A light moment for the audience |
| 12 | "Search YouTube for lofi music" | Opens YouTube results in a new tab |
| 13 | "Read my notes" | RUBY reads your note back |
| 14 | "Goodbye" | RUBY says goodbye and switches voice mode off |

While RUBY listens, point out the **core and wave reacting to your voice** and the **live subtitles** typing out each reply.

## If something goes wrong

- **Microphone not hearing you:** click a **suggestion chip** or type into the box. Every feature works without voice.
- **RUBY keeps listening to itself:** lower the volume slightly, or use headphones.
- **"Could not reach the weather service / Wikipedia":** the internet is down; continue with the timer, calculator, jokes and notes.
- **A website did not open:** the browser blocked the popup; click the link RUBY shows.
- **Anything stuck:** press **STOP**, then **ACTIVATE** again, or reload the page (your name and notes are kept).

## Talking points

- Built with **Spring Boot (Java)** on the backend and plain **HTML/CSS/JavaScript** on the frontend.
- Each skill is its own **command class**, so adding a new one is a single file.
- Uses free public APIs (**Open-Meteo**, **Wikipedia**): no paid keys.
- Your name and notes are stored **on your device**, and the app is **installable** (the base for the Android version).
- **140 automated tests** cover the commands and the API.
