// =========================
// RUBY SOUND EFFECTS
// Soft interface sounds generated with the Web Audio API.
// =========================

const RubySound = (() => {

    const STORAGE_KEY = "ruby.sound";

    let ctx = null;
    let enabled = localStorage.getItem(STORAGE_KEY) !== "off";

    // Browsers only allow audio after a user gesture, so the context is
    // created lazily on the first sound (which always follows a click/tap).
    function audio() {
        if (!ctx) {
            const Impl = window.AudioContext || window.webkitAudioContext;
            if (!Impl) return null;
            ctx = new Impl();
        }
        if (ctx.state === "suspended") ctx.resume();
        return ctx;
    }

    /** One soft sine note with a quick fade in and a smooth fade out. */
    function tone(freq, start, length, { volume = 0.08, type = "sine", slideTo = null } = {}) {
        const ac = audio();
        if (!ac) return;
        const t = ac.currentTime + start;

        const osc = ac.createOscillator();
        const gain = ac.createGain();
        osc.type = type;
        osc.frequency.setValueAtTime(freq, t);
        if (slideTo) osc.frequency.exponentialRampToValueAtTime(slideTo, t + length);

        gain.gain.setValueAtTime(0.0001, t);
        gain.gain.exponentialRampToValueAtTime(volume, t + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, t + length);

        osc.connect(gain).connect(ac.destination);
        osc.start(t);
        osc.stop(t + length + 0.05);
    }

    const sounds = {
        // Rising sweep: systems coming online.
        activate() {
            tone(220, 0, 0.35, { slideTo: 660, volume: 0.06 });
            tone(660, 0.3, 0.25, { volume: 0.05 });
            tone(990, 0.42, 0.35, { volume: 0.04 });
        },
        // Short blip: microphone open.
        listen() {
            tone(880, 0, 0.12, { volume: 0.05 });
        },
        // Two gentle notes: answer ready.
        reply() {
            tone(660, 0, 0.12, { volume: 0.04 });
            tone(990, 0.09, 0.18, { volume: 0.035 });
        },
        // Three-note chime: timer finished.
        timer() {
            [784, 988, 1175].forEach((f, i) => tone(f, i * 0.18, 0.5, { volume: 0.07 }));
            [784, 988, 1175].forEach((f, i) => tone(f, 0.8 + i * 0.18, 0.5, { volume: 0.07 }));
        },
        // Low falling tone: something went wrong.
        error() {
            tone(330, 0, 0.3, { slideTo: 180, volume: 0.06, type: "triangle" });
        },
        // Falling sweep: voice mode off.
        deactivate() {
            tone(660, 0, 0.3, { slideTo: 220, volume: 0.05 });
        }
    };

    return {
        play(name) {
            if (enabled && sounds[name]) {
                try {
                    sounds[name]();
                } catch (e) {
                    // Sound is decoration only; never let it break the assistant.
                }
            }
        },
        isEnabled: () => enabled,
        setEnabled(on) {
            enabled = on;
            localStorage.setItem(STORAGE_KEY, on ? "on" : "off");
        }
    };
})();
