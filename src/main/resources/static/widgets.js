// =========================
// RUBY WIDGETS
// Holographic result cards and countdown timers.
// All text is inserted with textContent: card data comes from the internet.
// =========================

/** Only allow normal web links and images (never javascript: URLs). */
function safeUrl(url) {
    return typeof url === "string" && /^https:\/\//i.test(url) ? url : null;
}

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
}

// -------------------------
// RESULT CARDS
// -------------------------

const RubyCards = (() => {

    const stage = document.getElementById("cardStage");
    const SHOW_FOR_MS = 30000;
    let hideTimer = null;

    const WEATHER_ICONS = [
        [/thunder/i, "⛈"], [/snow/i, "❄"], [/rain|drizzle|shower/i, "🌧"],
        [/fog/i, "🌫"], [/overcast|cloudy/i, "⛅"], [/clear night/i, "🌙"], [/clear/i, "☀"]
    ];

    function weatherIcon(description) {
        const match = WEATHER_ICONS.find(([pattern]) => pattern.test(description || ""));
        return match ? match[1] : "🌡";
    }

    function header(card) {
        const head = el("div", "card-head");
        head.append(el("div", "card-kicker", card.type === "weather" ? "WEATHER REPORT" : "KNOWLEDGE BASE"));
        const close = el("button", "card-close", "✕");
        close.type = "button";
        close.setAttribute("aria-label", "Close");
        close.addEventListener("click", hide);
        head.append(close);
        return head;
    }

    function weatherBody(card) {
        const body = el("div", "card-weather");
        const facts = card.facts || [];
        const temperature = facts.find(f => f.label === "Temperature");

        const main = el("div", "weather-main");
        main.append(el("div", "weather-icon", weatherIcon(card.subtitle)));
        main.append(el("div", "weather-temp", temperature ? temperature.value : "--"));
        body.append(main);

        body.append(el("div", "card-title", card.title));
        body.append(el("div", "card-subtitle", card.subtitle));

        const grid = el("div", "card-facts");
        facts.filter(f => f !== temperature).forEach(f => {
            const cell = el("div", "fact");
            cell.append(el("span", "fact-label", f.label), el("span", "fact-value", f.value));
            grid.append(cell);
        });
        body.append(grid);
        return body;
    }

    function knowledgeBody(card) {
        const body = el("div", "card-knowledge");
        const image = safeUrl(card.imageUrl);
        if (image) {
            const img = el("img", "card-image");
            img.src = image;
            img.alt = card.title;
            img.referrerPolicy = "no-referrer";
            img.addEventListener("error", () => img.remove());
            body.append(img);
        }
        const text = el("div", "card-text");
        text.append(el("div", "card-title", card.title));
        text.append(el("div", "card-subtitle", card.subtitle));
        text.append(el("p", "card-body", card.text));
        const link = safeUrl(card.link);
        if (link) {
            const a = el("a", "card-link", "READ MORE ›");
            a.href = link;
            a.target = "_blank";
            a.rel = "noopener noreferrer";
            text.append(a);
        }
        body.append(text);
        return body;
    }

    function show(card) {
        if (!card) return;
        clearTimeout(hideTimer);

        // "holo-<type>", not "card-<type>": the bodies use card-weather/card-knowledge.
        const box = el("div", "holo-card holo-" + card.type);
        box.append(header(card));
        box.append(card.type === "weather" ? weatherBody(card) : knowledgeBody(card));

        stage.replaceChildren(box);
        stage.classList.add("visible");
        hideTimer = setTimeout(hide, SHOW_FOR_MS);
    }

    function hide() {
        clearTimeout(hideTimer);
        stage.classList.remove("visible");
        setTimeout(() => {
            if (!stage.classList.contains("visible")) stage.replaceChildren();
        }, 400);
    }

    document.addEventListener("keydown", e => {
        if (e.key === "Escape") hide();
    });

    return { show, hide };
})();

// -------------------------
// TIMERS AND REMINDERS
// -------------------------

const RubyTimers = (() => {

    const STORAGE_KEY = "ruby.timers";
    const list = document.getElementById("timers");

    let timers = load();
    let onDone = () => {};

    function load() {
        try {
            const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
            // Drop timers that finished while the page was closed.
            return Array.isArray(saved) ? saved.filter(t => t.endsAt > Date.now()) : [];
        } catch (e) {
            return [];
        }
    }

    function save() {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(timers));
    }

    function format(ms) {
        const total = Math.max(0, Math.ceil(ms / 1000));
        const h = Math.floor(total / 3600);
        const m = Math.floor((total % 3600) / 60);
        const s = total % 60;
        const mm = String(m).padStart(2, "0");
        const ss = String(s).padStart(2, "0");
        return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
    }

    function render() {
        if (timers.length === 0) {
            list.replaceChildren(el("p", "empty", "NONE"));
            return;
        }
        const now = Date.now();
        list.replaceChildren(...timers.map(t => {
            const row = el("div", "timer-row");
            const top = el("div", "timer-top");
            top.append(el("span", "timer-label", t.label), el("span", "timer-left", format(t.endsAt - now)));
            const cancel = el("button", "timer-cancel", "✕");
            cancel.type = "button";
            cancel.setAttribute("aria-label", "Cancel " + t.label);
            cancel.addEventListener("click", () => remove(t.id));
            top.append(cancel);

            const bar = el("div", "timer-bar");
            const fill = el("div", "timer-fill");
            fill.style.width = Math.min(100, (100 * (t.endsAt - now)) / (t.seconds * 1000)) + "%";
            bar.append(fill);

            row.append(top, bar);
            return row;
        }));
    }

    function remove(id) {
        timers = timers.filter(t => t.id !== id);
        save();
        render();
    }

    function tick() {
        const now = Date.now();
        const finished = timers.filter(t => t.endsAt <= now);
        if (finished.length > 0) {
            timers = timers.filter(t => t.endsAt > now);
            save();
            finished.forEach(t => onDone(t));
        }
        render();
    }

    setInterval(tick, 1000);
    render();

    return {
        start(spec) {
            timers.push({
                id: Date.now() + "-" + Math.random().toString(36).slice(2, 7),
                label: spec.label,
                doneText: spec.doneText,
                seconds: spec.seconds,
                endsAt: Date.now() + spec.seconds * 1000
            });
            save();
            render();
        },
        cancelAll() {
            timers = [];
            save();
            render();
        },
        onFinished(callback) {
            onDone = callback;
        }
    };
})();
