// =========================
// RUBY VOICE
// Picks the most natural-sounding female English voice the browser
// offers, remembers the user's own choice, and speaks gently.
// =========================

const RubyVoice = (() => {

    const STORAGE_KEY = "ruby.voice";

    // Female voice names across Microsoft (Edge), Apple (Safari/Chrome on
    // Mac), Google (Chrome) and Windows.
    const FEMALE = [
        "aria", "jenny", "ava", "emma", "michelle", "ana", "sonia", "libby", "maisie",
        "neerja", "natasha", "clara", "molly", "emily", "zoe", "allison", "samantha",
        "susan", "serena", "kate", "karen", "moira", "tessa", "veena", "isha", "tara",
        "fiona", "victoria", "kathy", "shelley", "sandy", "flo", "nicky", "joanna",
        "salli", "kimberly", "ivy", "kendra", "hazel", "heera", "catherine", "linda",
        "zira", "jane", "nancy", "sara", "ashley", "amber", "cora", "elizabeth", "female"
    ];

    // Joke and robotic voices that ship with macOS: never pick these automatically.
    const NOVELTY = [
        "whisper", "bubbles", "bells", "boing", "bad news", "good news", "cellos",
        "wobble", "jester", "organ", "superstar", "trinoids", "zarvox", "bahh",
        "grandma", "grandpa", "albert", "fred", "junior", "ralph"
    ];

    const MALE = [
        "daniel", "alex", "aman", "rishi", "reed", "rocko", "eddy", "guy", "davis",
        "tony", "jason", "ryan", "thomas", "prabhat", "ravi", "george", "mark", "david",
        "evan", "nathan", "tom", "oliver", "arthur", "male"
    ];

    // Older synthetic voices that sound flat: usable, but rank them last.
    const FLAT = ["flo", "sandy", "shelley", "kathy", "eddy", "reed", "rocko"];

    // Smoother built-in Apple voices: best choice when nothing better is installed.
    const SMOOTH = ["samantha", "moira", "karen", "tessa", "tara", "veena", "isha", "serena"];

    let voices = [];
    const listeners = [];

    function load() {
        voices = speechSynthesis.getVoices().filter(v => /^en([-_]|$)/i.test(v.lang));
        listeners.forEach(fn => fn());
    }

    const hasWord = (name, words) =>
        words.some(w => new RegExp("\\b" + w + "\\b", "i").test(name));

    /** Higher = more natural and closer to a soft female assistant voice. */
    function score(voice) {
        const name = voice.name;
        if (hasWord(name, NOVELTY)) return -1000;

        let s = 0;
        if (/natural|neural/i.test(name)) s += 60;          // Edge: "Microsoft Aria Online (Natural)"
        else if (/premium/i.test(name)) s += 50;             // Apple downloadable voices
        else if (/enhanced/i.test(name)) s += 40;
        else if (/^google/i.test(name)) s += 30;             // Chrome's online voices
        else if (/online/i.test(name)) s += 25;

        if (hasWord(name, FEMALE)) s += 30;
        if (hasWord(name, MALE)) s -= 40;
        if (hasWord(name, FLAT)) s -= 15;
        if (hasWord(name, SMOOTH)) s += 5;

        if (/^en[-_](us|gb|in|ie|au)/i.test(voice.lang)) s += 2;
        return s;
    }

    function ranked() {
        return [...voices].sort((a, b) => score(b) - score(a));
    }

    function current() {
        const saved = localStorage.getItem(STORAGE_KEY);
        return voices.find(v => v.name === saved) || ranked()[0] || null;
    }

    function choose(name) {
        localStorage.setItem(STORAGE_KEY, name);
    }

    /** Natural voices sound best near normal speed; basic ones are calmer slightly slower. */
    function settingsFor(voice) {
        const natural = voice && score(voice) >= 40;
        return { rate: natural ? 0.95 : 0.9, pitch: natural ? 1.05 : 1.0, volume: 0.9 };
    }

    /** Words that voices read badly: all-caps RUBY gets spelled out letter by letter. */
    function prepare(text) {
        return text
            .replace(/\bRUBY\b/g, "Ruby")
            .replace(/\bChatGPT\b/g, "Chat G P T")
            .replace(/\bAI\b/g, "A.I.");
    }

    /** Short sentences sound more natural and avoid Chrome cutting off long speech. */
    function sentences(text) {
        return prepare(text)
            .split(/(?<=[.!?])\s+/)
            .map(s => s.trim())
            .filter(Boolean);
    }

    /**
     * Speaks text; calls onStart once and onDone once (finished, failed or cancelled).
     * Returns the utterances so callers can keep references alive (Chrome GC bug).
     */
    function say(text, { onStart = () => {}, onDone = () => {} } = {}) {
        const voice = current();
        const settings = settingsFor(voice);
        const parts = sentences(text);
        let done = false;
        const finish = () => { if (!done) { done = true; onDone(); } };

        if (parts.length === 0) {
            finish();
            return [];
        }

        const utterances = parts.map((part, i) => {
            const u = new SpeechSynthesisUtterance(part);
            if (voice) {
                u.voice = voice;
                u.lang = voice.lang;
            }
            u.rate = settings.rate;
            u.pitch = settings.pitch;
            u.volume = settings.volume;
            if (i === 0) u.onstart = onStart;
            if (i === parts.length - 1) u.onend = finish;
            u.onerror = finish;
            return u;
        });

        speechSynthesis.cancel();
        utterances.forEach(u => speechSynthesis.speak(u));
        return utterances;
    }

    if ("speechSynthesis" in window) {
        load();
        speechSynthesis.addEventListener("voiceschanged", load);
    }

    return {
        ranked,
        current,
        choose,
        say,
        score,
        onVoicesChanged: fn => listeners.push(fn),
        isNatural: voice => score(voice) >= 40
    };
})();
