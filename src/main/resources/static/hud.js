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

    console.log(startupSound);

    if(startupSound){

        startupSound.volume = 0.7;

        startupSound.play()
            .then(() => {

                console.log(
                    "Startup sound playing"
                );

            })
            .catch(error => {

                console.log(
                    "Autoplay blocked"
                );

                console.log(error);

            });
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
        "WELCOME YASH"
    ];

    let index = 0;

    const interval = setInterval(() => {

        bootText.innerHTML +=
            messages[index] +
            "<br>";

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

    window.addEventListener("resize", resizeCanvas);

    const mouse = {
        x: null,
        y: null
    };

    window.addEventListener("mousemove", (e) => {

        mouse.x = e.clientX;
        mouse.y = e.clientY;

    });

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

            if (mouse.x && mouse.y) {

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

            ctx.arc(
                this.x,
                this.y,
                this.radius,
                0,
                Math.PI * 2
            );

            ctx.fillStyle = "#ffb400";

            ctx.shadowBlur = 8;
            ctx.shadowColor = "#ff3b1f";

            ctx.fill();
        }
    }

    for (let i = 0; i < 60; i++) {

        particles.push(
            new Particle()
        );
    }

    function connectParticles() {

        for (let a = 0; a < particles.length; a++) {

            for (let b = a; b < particles.length; b++) {

                const dx =
                    particles[a].x - particles[b].x;

                const dy =
                    particles[a].y - particles[b].y;

                const distance =
                    Math.sqrt(dx * dx + dy * dy);

                if (distance < 120) {

                    ctx.beginPath();

                    ctx.strokeStyle =
                        `rgba(255,180,0,${
                            1 - distance / 120
                        })`;

                    ctx.lineWidth = 0.5;

                    ctx.moveTo(
                        particles[a].x,
                        particles[a].y
                    );

                    ctx.lineTo(
                        particles[b].x,
                        particles[b].y
                    );

                    ctx.stroke();
                }
            }
        }
    }

    function animate() {

        ctx.clearRect(
            0,
            0,
            width,
            height
        );

        particles.forEach(p => {

            p.update();
            p.draw();

        });

        connectParticles();

        setTimeout(() => {

            requestAnimationFrame(
                animate
            );

        }, 30);
    }

    animate();
    // CLOCK

    setInterval(() => {

        const clock =
            document.getElementById("clock");

        if(clock){

            clock.innerText =
                new Date()
                    .toLocaleTimeString();
        }

    },1000);

// UPTIME

    const uptimeElement =
        document.getElementById("uptime");

    const startTime =
        Date.now();

    setInterval(() => {

        if(!uptimeElement) return;

        const elapsed =
            Math.floor(
                (Date.now() - startTime) / 1000
            );

        const hours =
            String(
                Math.floor(elapsed / 3600)
            ).padStart(2,"0");

        const minutes =
            String(
                Math.floor(
                    (elapsed % 3600) / 60
                )
            ).padStart(2,"0");

        const seconds =
            String(
                elapsed % 60
            ).padStart(2,"0");

        uptimeElement.innerText =
            `${hours}:${minutes}:${seconds}`;

    },1000);

// CPU + MEMORY

    setInterval(() => {

        const cpu =
            document.getElementById(
                "cpuLoad"
            );

        const memory =
            document.getElementById(
                "memoryUsage"
            );

        const latency =
            document.getElementById(
                "latency"
            );

        if(cpu){

            cpu.innerText =
                Math.floor(
                    15 + Math.random()*25
                ) + "%";
        }

        if(memory){

            memory.innerText =
                Math.floor(
                    25 + Math.random()*35
                ) + "%";
        }

        if(latency){

            latency.innerText =
                Math.floor(
                    8 + Math.random()*15
                ) + "ms";
        }

    },2000);
    const blips =
        document.getElementById(
            "blips"
        );

    setInterval(() => {

        if(!blips) return;

        const blip =
            document.createElement("div");

        blip.className =
            "blip";

        blip.style.left =
            Math.random() * 170 + "px";

        blip.style.top =
            Math.random() * 170 + "px";

        blips.appendChild(blip);

        setTimeout(() => {

            blip.remove();

        },2000);

    },1000);
}

// =========================
// CIRCUIT BOARD BACKGROUND
// =========================

const traces = [];

for(let i = 0; i < 180; i++){

    traces.push({

        x: Math.random() * width,

        y: Math.random() * height,

        length:
            60 + Math.random() * 180,

        horizontal:
            Math.random() > 0.5,

        speed:
            0.2 + Math.random() * 0.5,

        offset:
            Math.random()
    });
}

function drawGrid(){

    ctx.strokeStyle =
        "rgba(255,180,0,0.03)";

    ctx.lineWidth = 1;

    for(let x = 0; x < width; x += 50){

        ctx.beginPath();

        ctx.moveTo(x,0);

        ctx.lineTo(x,height);

        ctx.stroke();
    }

    for(let y = 0; y < height; y += 50){

        ctx.beginPath();

        ctx.moveTo(0,y);

        ctx.lineTo(width,y);

        ctx.stroke();
    }
}

function drawCircuits(){

    traces.forEach(trace => {

        ctx.beginPath();

        ctx.strokeStyle =
            "rgba(255,180,0,0.12)";

        ctx.lineWidth = 1.2;

        ctx.moveTo(
            trace.x,
            trace.y
        );

        if(trace.horizontal){

            ctx.lineTo(
                trace.x + trace.length,
                trace.y
            );

        }else{

            ctx.lineTo(
                trace.x,
                trace.y + trace.length
            );
        }

        ctx.stroke();

        const progress =
            (
                Date.now() *
                trace.speed *
                0.0002
                +
                trace.offset
            ) % 1;

        let px;
        let py;

        if(trace.horizontal){

            px =
                trace.x +
                trace.length *
                progress;

            py =
                trace.y;

        }else{

            px =
                trace.x;

            py =
                trace.y +
                trace.length *
                progress;
        }

        ctx.beginPath();

        ctx.arc(
            px,
            py,
            2.5,
            0,
            Math.PI * 2
        );

        ctx.fillStyle =
            "#ffb400";

        ctx.shadowBlur = 20;

        ctx.shadowColor =
            "#ff3b1f";

        ctx.fill();
    });
}

function drawCenterPulse(){

    const cx = width / 2;

    const cy = height / 2;

    const radius =
        150 +
        Math.sin(
            Date.now() * 0.002
        ) * 10;

    const gradient =
        ctx.createRadialGradient(
            cx,
            cy,
            0,
            cx,
            cy,
            radius
        );

    gradient.addColorStop(
        0,
        "rgba(255,180,0,0.08)"
    );

    gradient.addColorStop(
        1,
        "rgba(255,180,0,0)"
    );

    ctx.fillStyle =
        gradient;

    ctx.beginPath();

    ctx.arc(
        cx,
        cy,
        radius,
        0,
        Math.PI * 2
    );

    ctx.fill();
}

function animate(){

    ctx.clearRect(
        0,
        0,
        width,
        height
    );

    drawCenterPulse();

    drawGrid();

    drawCircuits();

    requestAnimationFrame(
        animate
    );
}

animate();