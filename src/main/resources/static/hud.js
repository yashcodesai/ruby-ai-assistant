const canvas = document.getElementById('neuralCanvas');
window.addEventListener(
    "load",
    startBootSequence
);

function startBootSequence(){
    const startupSound =
        document.getElementById(
            "startupSound"
        );

    if(startupSound){

        startupSound.volume = 0.7;

        // Browsers block autoplay until the user interacts with the page;
        // that is expected, so fail quietly.
        startupSound.play().catch(() => {});
    }

    const bootScreen =
        document.getElementById(
            "bootScreen"
        );

    const bootText =
        document.getElementById(
            "bootText"
        );

    const progressBar =
        document.getElementById(
            "progressBar"
        );

    const mainUI =
        document.getElementById(
            "mainUI"
        );

    const messages = [

        "INITIALIZING RUBY AI...",
        "LOADING VOICE ENGINE...",
        "ESTABLISHING NETWORK...",
        "LOADING SECURITY MODULE...",
        "CONNECTING AI CORE...",
        "SYSTEM READY",
        "WELCOME"
    ];

    // Returning users (same browser session) are welcomed by name.
    fetch("/api/session")
        .then(res => res.ok ? res.json() : null)
        .then(session => {
            if(session && session.userName){
                messages[messages.length - 1] =
                    "WELCOME BACK " + session.userName.toUpperCase();
            }
        })
        .catch(() => {});

    let index = 0;

    const interval = setInterval(() => {

        const line = document.createElement("div");
        line.textContent = messages[index];
        bootText.appendChild(line);

        progressBar.style.width =
            ((index + 1) /
                messages.length)
            * 100 + "%";

        index++;

        if(index >= messages.length){

            clearInterval(
                interval
            );

            setTimeout(() => {

                bootScreen.style.opacity =
                    "0";

                setTimeout(() => {

                    bootScreen.remove();

                    mainUI.style.opacity =
                        "1";

                    // app.js starts the conversation (asks the user's name).
                    window.dispatchEvent(new Event("ruby:booted"));

                },1000);

            },1000);
        }

    },700);
}
if (canvas) {

    const ctx = canvas.getContext("2d");

    let width;
    let height;

    function resizeCanvas() {

        width = canvas.width = window.innerWidth;
        height = canvas.height = window.innerHeight;
    }

    resizeCanvas();

    window.addEventListener("resize", () => {
        resizeCanvas();
        createTraces();
    });

    const mouse = {
        x: null,
        y: null
    };

    window.addEventListener("mousemove", (e) => {

        mouse.x = e.clientX;
        mouse.y = e.clientY;

    });

    // =========================
    // NEURAL PARTICLES
    // =========================

    const particles = [];

    class Particle {

        constructor() {

            this.x = Math.random() * width;
            this.y = Math.random() * height;

            this.vx = (Math.random() - 0.5) * 1.2;
            this.vy = (Math.random() - 0.5) * 1.2;

            this.radius = Math.random() * 2 + 1;
        }

        update() {

            this.x += this.vx;
            this.y += this.vy;

            if (this.x < 0 || this.x > width) {
                this.vx *= -1;
            }

            if (this.y < 0 || this.y > height) {
                this.vy *= -1;
            }

            // Keep particles on screen after the window shrinks.
            this.x = Math.min(Math.max(this.x, 0), width);
            this.y = Math.min(Math.max(this.y, 0), height);

            if (mouse.x !== null && mouse.y !== null) {

                const dx = mouse.x - this.x;
                const dy = mouse.y - this.y;

                const distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < 120) {

                    this.x -= dx * 0.003;
                    this.y -= dy * 0.003;
                }
            }
        }

        draw() {

            ctx.beginPath();

            ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);

            ctx.fillStyle = "#ffb400";

            ctx.shadowBlur = 8;
            ctx.shadowColor = "#ff3b1f";

            ctx.fill();
        }
    }

    for (let i = 0; i < 60; i++) {

        particles.push(new Particle());
    }

    function connectParticles() {

        // No glow on lines: shadowBlur on hundreds of strokes is very slow.
        ctx.shadowBlur = 0;
        ctx.lineWidth = 0.5;

        for (let a = 0; a < particles.length; a++) {

            for (let b = a + 1; b < particles.length; b++) {

                const dx = particles[a].x - particles[b].x;
                const dy = particles[a].y - particles[b].y;

                const distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < 120) {

                    ctx.beginPath();

                    ctx.strokeStyle =
                        `rgba(255,180,0,${1 - distance / 120})`;

                    ctx.moveTo(particles[a].x, particles[a].y);
                    ctx.lineTo(particles[b].x, particles[b].y);

                    ctx.stroke();
                }
            }
        }
    }

    // =========================
    // CIRCUIT BOARD BACKGROUND
    // =========================

    let traces = [];

    function createTraces() {

        // Scale with screen area: ~80 traces on a 1440x900 screen.
        const count = Math.round(width * height / 16000);

        traces = [];

        for (let i = 0; i < count; i++) {

            traces.push({
                x: Math.random() * width,
                y: Math.random() * height,
                length: 60 + Math.random() * 180,
                horizontal: Math.random() > 0.5,
                speed: 0.2 + Math.random() * 0.5,
                offset: Math.random()
            });
        }
    }

    createTraces();

    function drawGrid() {

        ctx.shadowBlur = 0;
        ctx.strokeStyle = "rgba(255,180,0,0.03)";
        ctx.lineWidth = 1;

        ctx.beginPath();

        for (let x = 0; x < width; x += 50) {
            ctx.moveTo(x, 0);
            ctx.lineTo(x, height);
        }

        for (let y = 0; y < height; y += 50) {
            ctx.moveTo(0, y);
            ctx.lineTo(width, y);
        }

        ctx.stroke();
    }

    function drawCircuits(now) {

        ctx.shadowBlur = 0;
        ctx.strokeStyle = "rgba(255,180,0,0.12)";
        ctx.lineWidth = 1.2;

        ctx.beginPath();

        traces.forEach(trace => {

            ctx.moveTo(trace.x, trace.y);

            if (trace.horizontal) {
                ctx.lineTo(trace.x + trace.length, trace.y);
            } else {
                ctx.lineTo(trace.x, trace.y + trace.length);
            }
        });

        ctx.stroke();

        ctx.fillStyle = "#ffb400";
        ctx.shadowBlur = 12;
        ctx.shadowColor = "#ff3b1f";

        traces.forEach(trace => {

            const progress =
                (now * trace.speed * 0.0002 + trace.offset) % 1;

            const px = trace.horizontal
                ? trace.x + trace.length * progress
                : trace.x;

            const py = trace.horizontal
                ? trace.y
                : trace.y + trace.length * progress;

            ctx.beginPath();
            ctx.arc(px, py, 2.5, 0, Math.PI * 2);
            ctx.fill();
        });
    }

    function drawCenterPulse(now) {

        const cx = width / 2;
        const cy = height / 2;

        const radius = 150 + Math.sin(now * 0.002) * 10;

        const gradient =
            ctx.createRadialGradient(cx, cy, 0, cx, cy, radius);

        gradient.addColorStop(0, "rgba(255,180,0,0.08)");
        gradient.addColorStop(1, "rgba(255,180,0,0)");

        ctx.shadowBlur = 0;
        ctx.fillStyle = gradient;

        ctx.beginPath();
        ctx.arc(cx, cy, radius, 0, Math.PI * 2);
        ctx.fill();
    }

    // One loop for all layers (two loops would each clear the other's
    // drawing). Capped at ~30 fps to keep CPU use low.
    let lastFrame = 0;

    function animate(now) {

        requestAnimationFrame(animate);

        if (now - lastFrame < 33) return;
        lastFrame = now;

        ctx.clearRect(0, 0, width, height);

        drawCenterPulse(now);
        drawGrid();
        drawCircuits(now);

        particles.forEach(p => {
            p.update();
            p.draw();
        });

        connectParticles();
    }

    requestAnimationFrame(animate);
}

// CLOCK

const clock = document.getElementById("clock");

function updateClock() {
    if (clock) clock.textContent = new Date().toLocaleTimeString();
}

updateClock();
setInterval(updateClock, 1000);

// UPTIME

const uptimeElement = document.getElementById("uptime");
const startTime = Date.now();

setInterval(() => {

    if (!uptimeElement) return;

    const elapsed = Math.floor((Date.now() - startTime) / 1000);

    const hours = String(Math.floor(elapsed / 3600)).padStart(2, "0");
    const minutes = String(Math.floor((elapsed % 3600) / 60)).padStart(2, "0");
    const seconds = String(elapsed % 60).padStart(2, "0");

    uptimeElement.textContent = `${hours}:${minutes}:${seconds}`;

}, 1000);

// CPU + MEMORY (decorative values) and real backend LATENCY

const cpu = document.getElementById("cpuLoad");
const memory = document.getElementById("memoryUsage");
const latency = document.getElementById("latency");
const serverStatus = document.getElementById("serverStatus");
const statusDot = document.querySelector(".status .dot");

setInterval(() => {

    if (cpu) cpu.textContent = Math.floor(15 + Math.random() * 25) + "%";
    if (memory) memory.textContent = Math.floor(25 + Math.random() * 35) + "%";

}, 2000);

// Pings the backend so ONLINE/OFFLINE and latency are real, not invented.
async function checkServer() {

    const started = performance.now();

    try {
        const res = await fetch("/api/hello", { cache: "no-store" });
        if (!res.ok) throw new Error("HTTP " + res.status);

        if (latency) latency.textContent = Math.round(performance.now() - started) + "ms";
        if (serverStatus) serverStatus.textContent = "RUBY ONLINE";
        if (statusDot) statusDot.classList.remove("offline");

    } catch (error) {
        if (latency) latency.textContent = "--";
        if (serverStatus) serverStatus.textContent = "RUBY OFFLINE";
        if (statusDot) statusDot.classList.add("offline");
    }
}

checkServer();
setInterval(checkServer, 5000);

// RADAR BLIPS

const blips = document.getElementById("blips");

setInterval(() => {

    if (!blips || document.hidden) return;

    const blip = document.createElement("div");

    blip.className = "blip";
    blip.style.left = Math.random() * 170 + "px";
    blip.style.top = Math.random() * 170 + "px";

    blips.appendChild(blip);

    setTimeout(() => blip.remove(), 2000);

}, 1000);
