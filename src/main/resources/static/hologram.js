// =========================
// RUBY AVATAR HOLOGRAM
// A rotating, full-body female light-figure in a long gown, drawn in code:
// the body is built from stacked elliptical contour rings (no 3D models or
// libraries), rendered as glowing points with a bright holographic rim,
// standing on a projector disc.
// =========================

const RubyHologram = (() => {

    const canvas = document.getElementById("hologramCanvas");
    const stateOut = document.getElementById("holoState");
    if (!canvas) return { setActivity() {} };

    const ctx = canvas.getContext("2d");
    const TAU = Math.PI * 2;
    const FRAME_MS = 33;           // ~30 fps
    const SPIN_PER_SECOND = 0.45;  // radians per second
    const MIN_HEIGHT = 110;        // below this the panel is too short to draw

    // Every point: [x, y, z, normalX, normalZ, theta, sway, isHair]
    // y = 0 at the hem, ~1 at the top of the head. "sway" lets the gown hem ripple.
    const points = [];

    function ring(cx, y, cz, rx, rz, density, sway = 0) {
        const perimeter = TAU * Math.sqrt((rx * rx + rz * rz) / 2);
        const n = Math.max(10, Math.round(perimeter * density));
        for (let i = 0; i < n; i++) {
            const t = (i / n) * TAU;
            const nx = Math.cos(t) / rx;
            const nz = Math.sin(t) / rz;
            const len = Math.hypot(nx, nz);
            points.push([cx + rx * Math.cos(t), y, cz + rz * Math.sin(t), nx / len, nz / len, t, sway, 0]);
        }
    }

    /** Half ring at the back (z < 0), for hair falling behind the shoulders. */
    function backArc(cx, y, cz, rx, rz, density) {
        const n = Math.max(6, Math.round(Math.PI * Math.sqrt((rx * rx + rz * rz) / 2) * density));
        for (let i = 0; i <= n; i++) {
            const t = Math.PI + (i / n) * Math.PI;
            const nx = Math.cos(t) / rx;
            const nz = Math.sin(t) / rz;
            const len = Math.hypot(nx, nz);
            points.push([cx + rx * Math.cos(t), y, cz + rz * Math.sin(t), nx / len, nz / len, t, 0, 1]);
        }
    }

    const smooth = t => t * t * (3 - 2 * t);

    // ---- Body: gown, waist, bodice, shoulders, neck (heights 0..0.84) ----
    // [height, half-width, half-depth]
    const PROFILE = [
        [0.00, 0.170, 0.150],  // gown hem (A-line flare)
        [0.10, 0.145, 0.125],
        [0.25, 0.115, 0.098],
        [0.40, 0.098, 0.078],  // hips
        [0.50, 0.064, 0.052],  // waist
        [0.57, 0.070, 0.054],
        [0.64, 0.083, 0.070],  // bodice
        [0.70, 0.085, 0.058],
        [0.745, 0.098, 0.050], // shoulders
        [0.765, 0.075, 0.045], // shoulder slope
        [0.79, 0.030, 0.030],  // base of neck
        [0.84, 0.024, 0.026]   // neck
    ];

    function profileAt(y) {
        for (let i = 0; i < PROFILE.length - 1; i++) {
            const [y0, rx0, rz0] = PROFILE[i];
            const [y1, rx1, rz1] = PROFILE[i + 1];
            if (y >= y0 && y <= y1) {
                const t = smooth((y - y0) / (y1 - y0));
                return [rx0 + (rx1 - rx0) * t, rz0 + (rz1 - rz0) * t];
            }
        }
        return null;
    }

    for (let y = 0; y <= 0.84; y += 0.0075) {
        const [rx, rz] = profileAt(y);
        // the bodice sits slightly forward of the spine
        const cz = 0.007 * Math.exp(-Math.pow((y - 0.655) / 0.045, 2));
        const sway = y < 0.2 ? (0.2 - y) / 0.2 : 0;
        ring(0, y, cz, rx, rz, 130, sway);
    }

    // ---- Head (ellipsoid) ----
    const HEAD_Y = 0.895;
    for (let y = HEAD_Y - 0.056; y <= HEAD_Y + 0.058; y += 0.0075) {
        const k = (y - HEAD_Y) / 0.058;
        const s = Math.sqrt(Math.max(0, 1 - k * k));
        ring(0, y, 0.004, 0.042 * s, 0.05 * s, 150);
    }

    // ---- Hair: a shell over the crown, then long hair down the back ----
    for (let y = HEAD_Y - 0.02; y <= HEAD_Y + 0.066; y += 0.006) {
        const k = (y - HEAD_Y) / 0.068;
        const s = Math.sqrt(Math.max(0, 1 - k * k));
        backArc(0, y, -0.006, 0.05 * s + 0.006, 0.058 * s + 0.006, 170);
    }
    for (let y = 0.66; y < HEAD_Y - 0.02; y += 0.006) {
        const t = (HEAD_Y - 0.02 - y) / (HEAD_Y - 0.02 - 0.66); // 0 at the head, 1 at the ends
        backArc(0, y, -0.024 - t * 0.014, 0.05 + t * 0.034, 0.034 + t * 0.014, 170);
    }

    // ---- Arms: shoulder -> elbow -> wrist, tapered, relaxed at the sides ----
    function limb(a, b, ra, rb) {
        const steps = Math.max(4, Math.round((a[1] - b[1]) / 0.0075));
        for (let i = 0; i <= steps; i++) {
            const t = i / steps;
            ring(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t,
                ra + (rb - ra) * t, ra + (rb - ra) * t, 150);
        }
    }
    [-1, 1].forEach(side => {
        limb([side * 0.096, 0.742, 0], [side * 0.112, 0.61, -0.006], 0.02, 0.015);
        limb([side * 0.112, 0.61, -0.006], [side * 0.118, 0.49, 0.02], 0.015, 0.011);
        // hand
        for (let y = 0.49; y >= 0.45; y -= 0.0075) {
            const k = (y - 0.47) / 0.02;
            const s = Math.sqrt(Math.max(0, 1 - k * k));
            ring(side * 0.12, y, 0.022, 0.009 * s + 0.002, 0.013 * s + 0.002, 150);
        }
    });

    // ---- Particles rising from the projector ----
    const particles = Array.from({ length: 28 }, () => newParticle(true));
    function newParticle(anywhere) {
        return {
            angle: Math.random() * TAU,
            radius: 0.05 + Math.random() * 0.17,
            y: anywhere ? Math.random() * 0.9 : 0,
            speed: 0.05 + Math.random() * 0.1
        };
    }

    // ---- State ----

    let size = { w: 0, h: 0 };
    let dpr = 1;
    let angle = 0.6;
    let level = 0;        // 0..1, how strongly RUBY is listening/speaking
    let mode = "standby";
    let glitch = null;    // { y0, y1, shift, until }
    let nextGlitch = performance.now() + 3000;

    function resize() {
        dpr = Math.min(window.devicePixelRatio || 1, 2);
        size = { w: canvas.clientWidth, h: canvas.clientHeight };
        canvas.width = Math.round(size.w * dpr);
        canvas.height = Math.round(size.h * dpr);
    }

    resize();
    if (window.ResizeObserver) new ResizeObserver(resize).observe(canvas);

    // ---- Drawing ----

    function draw(now, dt) {
        const W = canvas.width;
        const H = canvas.height;
        ctx.clearRect(0, 0, W, H);
        if (size.h < MIN_HEIGHT) return;

        // Figure fits the panel height; the disc sits at the bottom.
        const S = Math.min(H * 0.82, W * 2.1);
        const cx = W / 2;
        const baseY = H - S * 0.07;
        const cos = Math.cos(angle);
        const sin = Math.sin(angle);
        const boost = 1 + level * 0.8;

        // Light cone from the projector
        const cone = ctx.createLinearGradient(0, baseY, 0, baseY - S * 1.05);
        cone.addColorStop(0, `rgba(255,170,40,${0.16 * boost})`);
        cone.addColorStop(1, "rgba(255,170,40,0)");
        ctx.fillStyle = cone;
        ctx.beginPath();
        ctx.moveTo(cx - S * 0.24, baseY);
        ctx.lineTo(cx + S * 0.24, baseY);
        ctx.lineTo(cx + S * 0.13, baseY - S * 1.05);
        ctx.lineTo(cx - S * 0.13, baseY - S * 1.05);
        ctx.closePath();
        ctx.fill();

        // Projector disc: glowing concentric ellipses with rotating dashes
        ctx.save();
        ctx.lineWidth = 1.2 * dpr;
        [0.26, 0.2, 0.13].forEach((r, i) => {
            ctx.strokeStyle = i === 0 ? `rgba(255,59,31,${0.55 + level * 0.4})` : `rgba(255,180,0,${0.45 + level * 0.4})`;
            ctx.setLineDash(i === 1 ? [4 * dpr, 5 * dpr] : []);
            ctx.lineDashOffset = -now / 40;
            ctx.beginPath();
            ctx.ellipse(cx, baseY, S * r, S * r * 0.22, 0, 0, TAU);
            ctx.stroke();
        });
        ctx.restore();
        const discGlow = ctx.createRadialGradient(cx, baseY, 0, cx, baseY, S * 0.26);
        discGlow.addColorStop(0, `rgba(255,200,80,${0.35 * boost})`);
        discGlow.addColorStop(1, "rgba(255,120,0,0)");
        ctx.fillStyle = discGlow;
        ctx.beginPath();
        ctx.ellipse(cx, baseY, S * 0.26, S * 0.06, 0, 0, TAU);
        ctx.fill();

        ctx.globalCompositeOperation = "lighter";

        // Scanning band moving up the figure
        const scanY = ((now / 2600) % 1.15) - 0.05;
        // Occasional glitch: a horizontal slice shifts sideways for a moment
        if (now > nextGlitch) {
            const y0 = Math.random() * 0.9;
            glitch = { y0, y1: y0 + 0.03 + Math.random() * 0.06, shift: (Math.random() - 0.5) * 0.06, until: now + 120 };
            nextGlitch = now + 2500 + Math.random() * 4000;
        }
        const glitching = glitch && now < glitch.until;
        const flicker = glitching ? 0.6 : 1;

        // Figure points
        const dotSize = 1.25 * dpr;
        for (let i = 0; i < points.length; i++) {
            let [x, y, z, nx, nz, theta, sway, hair] = points[i];
            if (sway > 0) {
                // the hem of the gown ripples gently
                const ripple = 0.006 * sway * Math.sin(theta * 6 + now / 450);
                x += nx * ripple;
                z += nz * ripple;
            }
            const rx = x * cos + z * sin;
            const rz = -x * sin + z * cos;
            const rnz = -nx * sin + nz * cos;

            // light perspective
            const p = 1 / (1 - rz * 0.35);
            let sx = cx + rx * S * p;
            const sy = baseY - S * 0.02 - y * S;
            if (glitching && y > glitch.y0 && y < glitch.y1) sx += glitch.shift * S;

            const front = rz >= 0;
            const rim = 1 - Math.abs(rnz);                      // edges glow brightest
            const scan = Math.max(0, 1 - Math.abs(y - scanY) / 0.035);
            let alpha = (front ? 0.07 : 0.025) + rim * (front ? 0.6 : 0.22) + scan * 0.5 + hair * 0.18;
            alpha = Math.min(1, alpha * boost * flicker);

            const warm = Math.round(150 + rim * 90 + scan * 15);
            ctx.fillStyle = `rgba(255,${warm},${Math.round(40 + rim * 120)},${alpha})`;
            ctx.fillRect(sx - dotSize / 2, sy - dotSize / 2, dotSize, dotSize);
        }

        // Rising particles
        particles.forEach((pt, i) => {
            pt.y += pt.speed * dt * (1 + level);
            if (pt.y > 1) particles[i] = newParticle(false);
            const px = cx + Math.cos(pt.angle + angle) * pt.radius * S;
            const py = baseY - pt.y * S;
            const a = (1 - pt.y) * 0.8;
            ctx.fillStyle = `rgba(255,190,60,${a})`;
            ctx.fillRect(px - dpr, py - dpr, 2 * dpr, 2 * dpr);
        });

        ctx.globalCompositeOperation = "source-over";
    }

    // ---- Loop ----

    let last = 0;
    function frame(now) {
        requestAnimationFrame(frame);
        if (now - last < FRAME_MS || document.hidden) return;
        const dt = Math.min(0.1, (now - last) / 1000);
        last = now;
        const speed = mode === "processing" ? 2.2 : 1;
        angle = (angle + SPIN_PER_SECOND * speed * dt) % TAU;
        draw(now, dt);
    }
    requestAnimationFrame(frame);

    const LABELS = {
        listening: "LISTENING",
        processing: "ANALYZING",
        speaking: "TRANSMITTING"
    };

    return {
        /** Called by app.js every frame with the core's activity level (0..1) and state. */
        setActivity(newLevel, newMode) {
            level = Math.max(0, Math.min(1, newLevel || 0));
            if (newMode !== mode) {
                mode = newMode || "standby";
                if (stateOut) stateOut.textContent = LABELS[mode] || "STABLE";
            }
        }
    };
})();
