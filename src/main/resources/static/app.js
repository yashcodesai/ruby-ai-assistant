// =========================
// RUBY CONVERSATION
// Voice input, replies, subtitles, the voice-reactive core,
// and saving the user's name and notes on this device.
// =========================

const btn = document.getElementById("listenBtn");
const response = document.getElementById("response");
const history = document.getElementById("history");
const reactor = document.querySelector(".reactor");
const core = document.querySelector(".core");
const coreStatus = document.getElementById("coreStatus");
const commandForm = document.getElementById("commandForm");
const commandInput = document.getElementById("commandInput");
const voiceSelect = document.getElementById("voiceSelect");
const subYou = document.getElementById("subYou");
const subRuby = document.getElementById("subRuby");
const suggestions = document.getElementById("suggestions");
const soundBtn = document.getElementById("soundBtn");
const micState = document.getElementById("micState");
const voiceName = document.getElementById("voiceName");
const userLabel = document.getElementById("userLabel");
const noteCount = document.getElementById("noteCount");
const commandCount = document.getElementById("commandCount");

const MAX_HISTORY_ENTRIES = 20;
const PROFILE_KEY = "ruby.profile";
const TIME_ZONE = Intl.DateTimeFormat().resolvedOptions().timeZone;

const SUGGESTIONS = [
    "What's the weather?",
    "Who is APJ Abdul Kalam?",
    "Set a timer for 1 minute",
    "Search YouTube for lofi music",
    "Tell me a joke",
    "What can you do?"
];

const SpeechRecognitionImpl =
    window.SpeechRecognition || window.webkitSpeechRecognition;

const canListen = Boolean(SpeechRecognitionImpl);
const canSpeak = "speechSynthesis" in window;

let recognition = null;

// "active" = the user switched voice mode on; RUBY keeps listening
// after each reply until they press STOP.
let active = false;
let recognizing = false;
let busy = false;
let commandsThisSession = 0;

// RUBY's opening line. Browsers only allow speech after the user
// interacts, so it is spoken on the first ACTIVATE click.
let pendingIntro = null;

// Things RUBY must say when it is free (e.g. a timer finishing mid-reply).
const announcements = [];

// Chrome can garbage-collect an utterance mid-speech so "onend" never
// fires; keeping references and a watchdog timer prevents getting stuck.
let currentUtterances = [];
let speechWatchdog = null;
let turnId = 0;

// =========================
// SETUP
// =========================

if (canListen) {
    btn.addEventListener("click", toggleVoice);
} else {
    btn.disabled = true;
    btn.textContent = "VOICE UNAVAILABLE";
    setMicState("UNSUPPORTED");
    response.textContent =
        "Voice input is not supported in this browser. Type a command below, " +
        "or use Chrome or Edge for voice.";
}

if (canSpeak) {
    RubyVoice.onVoicesChanged(fillVoicePicker);
    fillVoicePicker();
    voiceSelect.addEventListener("change", () => {
        RubyVoice.choose(voiceSelect.value);
        showVoiceName();
        previewVoice();
    });
} else {
    voiceSelect.disabled = true;
}

commandForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const text = commandInput.value.trim();
    if (!text || busy) return;
    commandInput.value = "";
    submitTyped(text);
});

SUGGESTIONS.forEach(text => {
    const chip = document.createElement("button");
    chip.type = "button";
    chip.className = "chip";
    chip.textContent = text;
    chip.addEventListener("click", () => {
        if (!busy) submitTyped(text);
    });
    suggestions.append(chip);
});

soundBtn.addEventListener("click", () => {
    RubySound.setEnabled(!RubySound.isEnabled());
    showSoundState();
    RubySound.play("listen");
});
showSoundState();

RubyTimers.onFinished(timer => announce(timer.doneText, "timer"));

// hud.js fires this when the boot screen has finished.
window.addEventListener("ruby:booted", introduce);

showProfile(loadProfile());

// =========================
// PROFILE (saved on this device)
// =========================

function loadProfile() {
    try {
        const saved = JSON.parse(localStorage.getItem(PROFILE_KEY) || "{}");
        return {
            userName: typeof saved.userName === "string" ? saved.userName : null,
            notes: Array.isArray(saved.notes) ? saved.notes.filter(n => typeof n === "string") : []
        };
    } catch (e) {
        return { userName: null, notes: [] };
    }
}

function saveProfile(data) {
    if (!data) return;
    const profile = { userName: data.userName || null, notes: data.notes || [] };
    localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
    showProfile(profile);
}

function showProfile(profile) {
    userLabel.textContent = profile.userName ? profile.userName.toUpperCase() : "UNKNOWN";
    noteCount.textContent = String(profile.notes.length);
}

// =========================
// STATUS DISPLAYS
// =========================

function setState(state) {
    core.className = "core " + state;
    const labels = {
        listening: "LISTENING",
        processing: "THINKING",
        speaking: "SPEAKING"
    };
    coreStatus.textContent = labels[state] || "STANDBY";
    reactor.dataset.state = state;
}

function setMicState(text) {
    micState.textContent = text;
    micState.classList.toggle("alert", text === "BLOCKED" || text === "UNSUPPORTED");
    micState.classList.toggle("live", text === "LISTENING");
}

function showSoundState() {
    const on = RubySound.isEnabled();
    const icon = document.createElement("span");
    icon.className = "sound-icon";
    icon.textContent = on ? "🔊" : "🔇";
    const label = document.createElement("span");
    label.className = "sound-text";
    label.textContent = on ? " SOUND ON" : " SOUND OFF";
    soundBtn.replaceChildren(icon, label);
    soundBtn.setAttribute("aria-label", on ? "Sound on" : "Sound off");
    soundBtn.setAttribute("aria-pressed", String(on));
}

function showVoiceName() {
    voiceName.textContent = RubyVoice.shortName(RubyVoice.current());
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

// =========================
// SUBTITLES (typed out letter by letter)
// =========================

let typingTimer = null;

function showYou(text, interim = false) {
    subYou.textContent = text ? "YOU › " + text : "";
    subYou.classList.toggle("interim", interim);
}

function typeRuby(text) {
    clearInterval(typingTimer);
    subRuby.textContent = "";
    subRuby.classList.add("typing");
    if (!text) return;

    // Whole reply appears within ~2.5 seconds, however long it is.
    const step = Math.max(1, Math.ceil(text.length / 100));
    let shown = 0;
    typingTimer = setInterval(() => {
        shown = Math.min(text.length, shown + step);
        subRuby.textContent = "RUBY › " + text.slice(0, shown);
        if (shown >= text.length) {
            clearInterval(typingTimer);
            subRuby.classList.remove("typing");
        }
    }, 25);
}

// =========================
// VOICE-REACTIVE CORE
// The rings and wave follow the microphone volume while listening,
// and pulse with each spoken word while RUBY talks.
// =========================

let micStream = null;
let analyser = null;
let analyserData = null;
let level = 0;
let wordKick = 0;

async function startMicMeter() {
    if (micStream || !navigator.mediaDevices?.getUserMedia) return;
    try {
        micStream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const Impl = window.AudioContext || window.webkitAudioContext;
        const ac = new Impl();
        analyser = ac.createAnalyser();
        analyser.fftSize = 512;
        analyserData = new Uint8Array(analyser.fftSize);
        ac.createMediaStreamSource(micStream).connect(analyser);
        micStream.audioContext = ac;
    } catch (e) {
        // No meter (e.g. permission denied); recognition reports its own errors.
        micStream = null;
        analyser = null;
    }
}

function stopMicMeter() {
    if (!micStream) return;
    micStream.getTracks().forEach(t => t.stop());
    micStream.audioContext?.close();
    micStream = null;
    analyser = null;
}

function micVolume() {
    if (!analyser) return 0;
    analyser.getByteTimeDomainData(analyserData);
    let sum = 0;
    for (const v of analyserData) {
        const x = (v - 128) / 128;
        sum += x * x;
    }
    return Math.min(1, Math.sqrt(sum / analyserData.length) * 6);
}

function animateLevel(now) {
    requestAnimationFrame(animateLevel);

    let target = 0;
    const state = reactor.dataset.state;
    if (state === "listening") {
        target = micVolume();
    } else if (state === "speaking") {
        target = 0.25 + 0.15 * Math.sin(now / 90) + wordKick;
    } else if (state === "processing") {
        target = 0.15 + 0.1 * Math.sin(now / 150);
    }
    wordKick *= 0.9;

    level += (target - level) * 0.25;
    reactor.style.setProperty("--level", level.toFixed(3));
}

requestAnimationFrame(animateLevel);

// =========================
// CONVERSATION FLOW
// =========================

function toggleVoice() {
    if (active) {
        stopVoice();
        return;
    }
    active = true;
    btn.textContent = "STOP";
    btn.classList.add("active");
    RubySound.play("activate");
    startMicMeter();

    if (pendingIntro) {
        // First click: RUBY greets the user out loud, then listens.
        const intro = pendingIntro;
        pendingIntro = null;
        busy = true;
        speak(intro);
    } else {
        startListening();
    }
}

function stopVoice() {
    active = false;
    btn.textContent = "ACTIVATE";
    btn.classList.remove("active");
    if (recognition) recognition.abort();
    if (canSpeak) speechSynthesis.cancel();
    stopMicMeter();
    RubySound.play("deactivate");
    setMicState(canListen ? "OFF" : "UNSUPPORTED");
    if (!busy) {
        setState("standby");
        response.textContent = "Voice mode off. Awaiting command...";
    }
}

function submitTyped(text) {
    // Typing while the mic is open: stop listening so the two don't overlap.
    if (recognition && recognizing) recognition.abort();
    sendCommand(text);
}

async function introduce() {
    const profile = loadProfile();
    try {
        const res = await fetch("/api/intro", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                userName: profile.userName,
                notes: profile.notes,
                timeZone: TIME_ZONE
            })
        });
        const data = await res.json();
        saveProfile(data);
        response.textContent = "RUBY: " + data.response;
        addHistory("RUBY", data.response);
        typeRuby(data.response);
        pendingIntro = data.response;
    } catch (error) {
        console.error(error);
        typeRuby("I cannot reach my server right now.");
    }
}

function startListening() {
    if (!active || recognizing || busy) return;

    recognition = new SpeechRecognitionImpl();
    recognition.lang = "en-US";
    recognition.interimResults = true;
    recognition.maxAlternatives = 1;

    recognition.onresult = (event) => {
        let interim = "";
        for (let i = event.resultIndex; i < event.results.length; i++) {
            const result = event.results[i];
            if (result.isFinal) {
                sendCommand(result[0].transcript.trim());
                return;
            }
            interim += result[0].transcript;
        }
        // Live transcript while the user is still speaking.
        showYou(interim.trim(), true);
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
        RubySound.play("error");
        stopVoice();
        setMicState(blocked ? "BLOCKED" : "ERROR");
    };

    recognition.onend = () => {
        recognizing = false;
        // Keep listening through silence while voice mode is on.
        if (active && !busy) setTimeout(startListening, 300);
    };

    recognizing = true;
    response.textContent = "🎤 Listening...";
    setState("listening");
    setMicState("LISTENING");
    RubySound.play("listen");
    recognition.start();
}

async function sendCommand(text) {
    if (!text) return;
    busy = true;
    pendingIntro = null;
    response.textContent = "You said: " + text;
    showYou(text);
    addHistory("USER", text);
    setState("processing");
    if (active) setMicState("PAUSED");
    commandCount.textContent = String(++commandsThisSession);

    let reply;
    try {
        const res = await fetch("/api/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: text, timeZone: TIME_ZONE })
        });
        const data = await res.json().catch(() => null);
        if (!data || !data.response) {
            throw new Error("Unexpected response (HTTP " + res.status + ")");
        }
        reply = data;
    } catch (error) {
        console.error(error);
        busy = false;
        RubySound.play("error");
        response.textContent = "Failed to contact RUBY backend.";
        typeRuby("I cannot reach my server right now.");
        setState("standby");
        if (active) setTimeout(startListening, 1000);
        return;
    }

    if (reply.userName !== undefined) saveProfile(reply);
    RubySound.play("reply");
    response.textContent = "RUBY: " + reply.response;
    addHistory("RUBY", reply.response);
    typeRuby(reply.response);

    if (reply.card) RubyCards.show(reply.card);
    if (reply.timer) RubyTimers.start(reply.timer);
    if (reply.cancelTimers) RubyTimers.cancelAll();
    if (reply.stopListening && active) {
        active = false;
        btn.textContent = "ACTIVATE";
        btn.classList.remove("active");
        if (recognition) recognition.abort();
        stopMicMeter();
        setMicState("OFF");
    }
    if (reply.url) openWebsite(reply.url);
    speak(reply.response);
}

/** RUBY speaks up on its own (timer finished). Waits if it is busy. */
function announce(text, sound) {
    if (busy) {
        announcements.push({ text, sound });
        return;
    }
    busy = true;
    if (recognition && recognizing) recognition.abort();
    RubySound.play(sound);
    response.textContent = "RUBY: " + text;
    addHistory("RUBY", text);
    showYou("");
    typeRuby(text);
    // Give the chime a moment before speaking.
    setTimeout(() => speak(text), sound === "timer" ? 1400 : 0);
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

    if (announcements.length > 0) {
        const next = announcements.shift();
        announce(next.text, next.sound);
        return;
    }
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
        onWord: () => { wordKick = 0.35; },
        onDone: () => finishTurn(id)
    });

    // Roughly 13 characters a second at the calmer rate, plus slack for
    // voice loading, in case the browser never reports the end of speech.
    speechWatchdog = setTimeout(() => finishTurn(id), 6000 + text.length * 80);
}

// =========================
// VOICE PICKER
// =========================

function fillVoicePicker() {
    const voices = RubyVoice.ranked().filter(v => RubyVoice.score(v) > -1000);
    const selected = RubyVoice.current();
    voiceSelect.replaceChildren();
    showVoiceName();

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
