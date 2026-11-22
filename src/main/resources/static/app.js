const btn = document.getElementById("listenBtn");
const response = document.getElementById("response");
const history = document.getElementById("history");
const core = document.querySelector(".core");
const coreStatus = document.getElementById("coreStatus");
const commandForm = document.getElementById("commandForm");
const commandInput = document.getElementById("commandInput");
const voiceSelect = document.getElementById("voiceSelect");

const MAX_HISTORY_ENTRIES = 20;

const SpeechRecognitionImpl =
    window.SpeechRecognition || window.webkitSpeechRecognition;

const canListen = Boolean(SpeechRecognitionImpl);
const canSpeak = "speechSynthesis" in window;

let recognition = null;

// RUBY's opening line ("What is your name?"). Browsers only allow speech
// after the user interacts, so it is spoken on the first ACTIVATE click.
let pendingIntro = null;

// "active" = the user switched voice mode on; RUBY keeps listening
// after each reply until they press STOP.
let active = false;
let recognizing = false;
let busy = false;

// Chrome can garbage-collect an utterance mid-speech so "onend" never
// fires; keeping a reference and a watchdog timer prevents getting stuck.
let currentUtterances = [];
let speechWatchdog = null;
let turnId = 0;

if (canSpeak) {
    RubyVoice.onVoicesChanged(fillVoicePicker);
    fillVoicePicker();
    voiceSelect.addEventListener("change", () => {
        RubyVoice.choose(voiceSelect.value);
        previewVoice();
    });
} else {
    voiceSelect.disabled = true;
}

// hud.js fires this when the boot screen has finished.
window.addEventListener("ruby:booted", introduce);

if (canListen) {
    btn.addEventListener("click", toggleVoice);
} else {
    btn.disabled = true;
    btn.textContent = "VOICE UNAVAILABLE";
    response.textContent =
        "Voice input is not supported in this browser. Type a command below, " +
        "or use Chrome or Edge for voice.";
}

commandForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const text = commandInput.value.trim();
    if (!text || busy) return;
    commandInput.value = "";
    // Typing while the mic is open: stop listening so the two don't overlap.
    if (recognition && recognizing) recognition.abort();
    sendCommand(text);
});

function setState(state) {
    core.className = "core " + state;
    const labels = {
        listening: "LISTENING",
        processing: "THINKING",
        speaking: "SPEAKING"
    };
    coreStatus.textContent = labels[state] || "STANDBY";
}

function addHistory(sender, message) {
    const entry = document.createElement("p");
    const time = new Date().toLocaleTimeString();
    const who = document.createElement("b");
    who.textContent = sender;

    // textContent, never innerHTML: messages are untrusted text.
    entry.append(`[${time}] `, who, ` > ${message}`);
    history.appendChild(entry);

    while (history.children.length > MAX_HISTORY_ENTRIES) {
        history.removeChild(history.firstChild);
    }
    history.scrollTop = history.scrollHeight;
}

function toggleVoice() {
    if (active) {
        stopVoice();
        return;
    }
    active = true;
    btn.textContent = "STOP";
    if (pendingIntro) {
        // First click: RUBY asks the user's name out loud, then listens.
        const intro = pendingIntro;
        pendingIntro = null;
        busy = true;
        speak(intro);
    } else {
        startListening();
    }
}

async function introduce() {
    try {
        const res = await fetch("/api/intro", { method: "POST" });
        const data = await res.json();
        response.textContent = "RUBY: " + data.response;
        addHistory("RUBY", data.response);
        pendingIntro = data.response;
    } catch (error) {
        console.error(error);
    }
}

function fillVoicePicker() {
    const voices = RubyVoice.ranked().filter(v => RubyVoice.score(v) > -1000);
    const selected = RubyVoice.current();
    voiceSelect.replaceChildren();

    if (voices.length === 0) {
        voiceSelect.append(new Option("Default voice", ""));
        return;
    }
    voices.forEach(v => {
        const label = (RubyVoice.isNatural(v) ? "★ " : "") + v.name.replace(/^Microsoft /, "");
        voiceSelect.append(new Option(label, v.name, false, selected && v.name === selected.name));
    });
}

function previewVoice() {
    // Not while voice mode is on: the microphone would hear the sample.
    if (busy || active) return;
    RubyVoice.say("Hello, I am Ruby. This is how I will sound.");
}

function stopVoice() {
    active = false;
    btn.textContent = "ACTIVATE";
    if (recognition) recognition.abort();
    if (canSpeak) speechSynthesis.cancel();
    if (!busy) {
        setState("standby");
        response.textContent = "Voice mode off. Awaiting command...";
    }
}

function startListening() {
    if (!active || recognizing || busy) return;

    recognition = new SpeechRecognitionImpl();
    recognition.lang = "en-US";
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognition.onresult = (event) => {
        sendCommand(event.results[0][0].transcript);
    };

    recognition.onerror = (event) => {
        // Silence or an aborted session is normal; anything else is a real problem.
        if (event.error === "no-speech" || event.error === "aborted") return;

        console.warn("Recognition error:", event.error);
        const blocked =
            event.error === "not-allowed" || event.error === "service-not-allowed";
        response.textContent = blocked
            ? "Microphone access was blocked. Allow it in your browser settings, or type below."
            : "Microphone error: " + event.error;
        stopVoice();
    };

    recognition.onend = () => {
        recognizing = false;
        // Keep listening through silence while voice mode is on.
        if (active && !busy) setTimeout(startListening, 300);
    };

    recognizing = true;
    response.textContent = "🎤 Listening...";
    setState("listening");
    recognition.start();
}

async function sendCommand(text) {
    busy = true;
    pendingIntro = null;
    response.textContent = "You said: " + text;
    addHistory("USER", text);
    setState("processing");

    let reply;
    try {
        const res = await fetch("/api/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: text })
        });
        const data = await res.json().catch(() => null);
        if (!data || !data.response) {
            throw new Error("Unexpected response (HTTP " + res.status + ")");
        }
        reply = data;
    } catch (error) {
        console.error(error);
        busy = false;
        response.textContent = "Failed to contact RUBY backend.";
        setState("standby");
        if (active) setTimeout(startListening, 1000);
        return;
    }

    response.textContent = "RUBY: " + reply.response;
    addHistory("RUBY", reply.response);
    if (reply.stopListening && active) {
        active = false;
        btn.textContent = "ACTIVATE";
        if (recognition) recognition.abort();
    }
    if (reply.url) openWebsite(reply.url);
    speak(reply.response);
}

function openWebsite(url) {
    // Not using the "noopener" feature: with it, window.open always returns
    // null, which made every successful open look like a blocked popup.
    const opened = window.open(url, "_blank");
    if (opened) {
        opened.opener = null;
        return;
    }

    // Popup blockers may stop windows opened after an async call: offer a link.
    response.textContent = "Popup blocked. ";
    const link = document.createElement("a");
    link.href = url;
    link.target = "_blank";
    link.rel = "noopener";
    link.textContent = "Click to open " + url;
    response.appendChild(link);
}

function finishTurn(id) {
    // Ignore late events from an earlier turn (e.g. watchdog after onend).
    if (id !== turnId) return;
    turnId++;
    clearTimeout(speechWatchdog);
    currentUtterances = [];
    busy = false;
    if (active) {
        startListening();
    } else {
        setState("standby");
    }
}

function speak(text) {
    const id = turnId;
    if (!canSpeak) {
        finishTurn(id);
        return;
    }

    currentUtterances = RubyVoice.say(text, {
        onStart: () => setState("speaking"),
        onDone: () => finishTurn(id)
    });

    // Roughly 13 characters a second at the calmer rate, plus slack for
    // voice loading, in case the browser never reports the end of speech.
    speechWatchdog = setTimeout(() => finishTurn(id), 6000 + text.length * 80);
}
