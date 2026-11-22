// =========================
// RUBY AVATAR HOLOGRAM
// A rotating female light-figure in a long gown, drawn in code (no models
// or libraries). Rendered like a film hologram: evenly spaced thin contour
// slices, a crisp glowing silhouette, and a small HUD frame with readouts.
// =========================

const RubyHologram = (() => {

    const canvas = document.getElementById("hologramCanvas");
    const stateOut = document.getElementById("holoState");
    if (!canvas) return { setActivity() {} };

    const ctx = canvas.getContext("2d");
    const TAU = Math.PI * 2;
    const FRAME_MS = 33;           // ~30 fps
    const SPIN_PER_SECOND = 0.4;   // radians per second
    const MIN_HEIGHT = 90;         // below this there is no room to draw

    const GOLD = "255,180,0";
    const AMBER = "255,140,30";
    const RED = "255,59,31";

    // ---- Geometry (unit height: hem y = 0, top of head y ≈ 0.96) ----
    // Each slice is a horizontal ellipse: { y, cx, cz, rx, rz, part, sway, from, to }
    // "from/to" limit the arc (hair only covers the back of the head).

    const slices = [];
    const smooth = t => t * t * (3 - 2 * t);

    // Gown and body: [height, half-width, half-depth]
    const PROFILE = [
        [0.00, 0.160, 0.140],  // hem
        [0.12, 0.132, 0.114],
        [0.28, 0.104, 0.088],
        [0.41, 0.092, 0.074],  // hips
        [0.50, 0.060, 0.050],  // waist
        [0.58, 0.068, 0.054],
        [0.645, 0.078, 0.066], // bodice
        [0.70, 0.080, 0.056],
        [0.735, 0.100, 0.050], // shoulders
        [0.758, 0.072, 0.044],
        [0.782, 0.028, 0.028], // base of neck
        [0.83, 0.023, 0.025]   // neck
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
        return PROFILE[PROFILE.length - 1].slice(1);
    }

    /** Gentle hip sway (contrapposto) so the figure does not stand like a mannequin. */
    const swayX = y => 0.012 * Math.sin(Math.min(1, Math.max(0, (y - 0.15) / 0.65)) * Math.PI);

    // spacing between contour slices: wide enough to read as separate lines
    const BODY_STEP = 0.026;
    for (let y = 0; y <= 0.83; y += BODY_STEP) {
        const [rx, rz] = profileAt(y);
        slices.push({ y, cx: swayX(y), cz: 0.006 * Math.exp(-Math.pow((y - 0.645) / 0.045, 2)),
            rx, rz, part: "body", sway: y < 0.18 ? (0.18 - y) / 0.18 : 0, from: 0, to: TAU });
    }

    // Head, tilted very slightly
    const HEAD_Y = 0.885;
    for (let y = HEAD_Y - 0.052; y <= HEAD_Y + 0.056; y += 0.018) {
        const k = (y - HEAD_Y) / 0.056;
        const s = Math.sqrt(Math.max(0, 1 - k * k));
        slices.push({ y, cx: swayX(0.83) + 0.004, cz: 0.004, rx: 0.039 * s, rz: 0.046 * s,
            part: "head", sway: 0, from: 0, to: TAU });
    }

    // Hair: over the crown and falling down the back
    for (let y = HEAD_Y - 0.02; y <= HEAD_Y + 0.062; y += 0.016) {
        const k = (y - HEAD_Y) / 0.064;
        const s = Math.sqrt(Math.max(0, 1 - k * k));
        slices.push({ y, cx: swayX(0.83) + 0.004, cz: -0.004, rx: 0.046 * s + 0.005, rz: 0.054 * s + 0.005,
            part: "hair", sway: 0, from: Math.PI * 0.85, to: Math.PI * 2.15 });
    }
    for (let y = 0.67; y < HEAD_Y - 0.02; y += 0.016) {
        const t = (HEAD_Y - 0.02 - y) / (HEAD_Y - 0.02 - 0.67); // 0 at the head, 1 at the ends
        slices.push({ y, cx: swayX(y), cz: -0.018 - t * 0.012, rx: 0.05 + t * 0.036, rz: 0.034 + t * 0.012,
            part: "hair", sway: 0, from: Math.PI * 0.78, to: Math.PI * 2.22 });
    }

    // Arms: relaxed and slightly bent, hands near the hips
    function limb(a, b, ra, rb, step, side) {
        const n = Math.max(3, Math.round((a[1] - b[1]) / step));
        for (let i = 0; i <= n; i++) {
            const t = i / n;
            const r = ra + (rb - ra) * t;
            slices.push({ y: a[1] + (b[1] - a[1]) * t, cx: a[0] + (b[0] - a[0]) * t, cz: a[2] + (b[2] - a[2]) * t,
                rx: r, rz: r, part: "arm", side, sway: 0, from: 0, to: TAU });
        }
    }
    [-1, 1].forEach(side => {
        const hip = swayX(0.45);
        // angled a little away from the body so the arms read as arms
        limb([side * 0.098 + swayX(0.74), 0.73, 0], [side * 0.134 + hip, 0.6, -0.008], 0.019, 0.014, 0.026, side);
        limb([side * 0.134 + hip, 0.6, -0.008], [side * 0.146 + hip, 0.48, 0.022], 0.014, 0.011, 0.026, side);
    });

    // ---- State ----

    let dpr = 1;
    let cssH = 0;
    let angle = 0.6;
    let level = 0;        // 0..1, how strongly RUBY is listening/speaking
    let mode = "standby";

    function resize() {
        dpr = Math.min(window.devicePixelRatio || 1, 2);
        cssH = canvas.clientHeight;
        canvas.width = Math.round(canvas.clientWidth * dpr);
        canvas.height = Math.round(cssH * dpr);
    }

    resize();
    if (window.ResizeObserver) new ResizeObserver(resize).observe(canvas);

    // ---- Drawing ----

    /** Rotates a point around the vertical axis; returns [x, depth]. */
    function rotate(x, z, cos, sin) {
        return [x * cos + z * sin, -x * sin + z * cos];
    }

    function drawFrame(W, H, figLeft, figTop, figW, figH, now) {
        const c = 10 * dpr;
        ctx.strokeStyle = `rgba(${GOLD},0.55)`;
        ctx.lineWidth = 1 * dpr;
        ctx.beginPath();
        // corner brackets
        [[figLeft, figTop, 1, 1], [figLeft + figW, figTop, -1, 1],
         [figLeft, figTop + figH, 1, -1], [figLeft + figW, figTop + figH, -1, -1]].forEach(([x, y, dx, dy]) => {
            ctx.moveTo(x, y + dy * c);
            ctx.lineTo(x, y);
            ctx.lineTo(x + dx * c, y);
        });
        ctx.stroke();

        // height ruler on the left
        const rulerX = figLeft + 6 * dpr;
        ctx.strokeStyle = `rgba(${GOLD},0.35)`;
        ctx.beginPath();
        for (let i = 0; i <= 20; i++) {
            const y = figTop + 14 * dpr + (figH - 28 * dpr) * (i / 20);
            const len = i % 5 === 0 ? 6 * dpr : 3 * dpr;
            ctx.moveTo(rulerX, y);
            ctx.lineTo(rulerX + len, y);
        }
        ctx.stroke();

        // readouts
        ctx.font = `${7.5 * dpr}px Orbitron, sans-serif`;
        ctx.fillStyle = `rgba(${GOLD},0.75)`;
        ctx.textAlign = "right";
        const degrees = Math.round(((angle % TAU) + TAU) % TAU / TAU * 360);
        const sync = mode === "standby" ? 100 : Math.round(92 + level * 8);
        ctx.fillText("ID RUBY-01", figLeft + figW - 4 * dpr, figTop + 12 * dpr);
        ctx.fillText("ROT " + String(degrees).padStart(3, "0") + "°", figLeft + figW - 4 * dpr, figTop + 23 * dpr);
        ctx.fillText("SYNC " + sync + "%", figLeft + figW - 4 * dpr, figTop + figH - 6 * dpr);

        // blinking "live" dot
        if (Math.floor(now / 700) % 2 === 0) {
            ctx.fillStyle = `rgba(${RED},0.9)`;
            ctx.beginPath();
            ctx.arc(figLeft + 18 * dpr, figTop + 9 * dpr, 2 * dpr, 0, TAU);
            ctx.fill();
        }
    }

    function draw(now) {
        const W = canvas.width;
        const H = canvas.height;
        ctx.clearRect(0, 0, W, H);
        if (cssH < MIN_HEIGHT) return;

        const figH = H;
        const figW = Math.min(W, H * 1.05);
        const figLeft = (W - figW) / 2;
        const figTop = 0;

        // Figure takes ~80% of the frame height
        const S = H * 0.8;
        const cx = W / 2;
        const baseY = H - H * 0.1;
        const breathe = 1 + 0.006 * Math.sin(now / 900);
        const cos = Math.cos(angle);
        const sin = Math.sin(angle);
        const boost = 1 + level * 0.7;
        const flicker = Math.random() < 0.015 ? 0.75 : 1;

        drawFrame(W, H, figLeft, figTop, figW, figH, now);

        // Soft light column from the projector
        const cone = ctx.createLinearGradient(0, baseY, 0, baseY - S * 1.08);
        cone.addColorStop(0, `rgba(${AMBER},${0.13 * boost})`);
        cone.addColorStop(1, `rgba(${AMBER},0)`);
        ctx.fillStyle = cone;
        ctx.beginPath();
        ctx.moveTo(cx - S * 0.2, baseY);
        ctx.lineTo(cx + S * 0.2, baseY);
        ctx.lineTo(cx + S * 0.12, baseY - S * 1.08);
        ctx.lineTo(cx - S * 0.12, baseY - S * 1.08);
        ctx.closePath();
        ctx.fill();

        // Projector disc
        ctx.lineWidth = 1 * dpr;
        [[0.22, RED, 0.6], [0.16, GOLD, 0.5], [0.1, GOLD, 0.35]].forEach(([r, color, a], i) => {
            ctx.strokeStyle = `rgba(${color},${a + level * 0.3})`;
            ctx.setLineDash(i === 1 ? [3 * dpr, 4 * dpr] : []);
            ctx.lineDashOffset = -now / 50;
            ctx.beginPath();
            ctx.ellipse(cx, baseY, S * r, S * r * 0.2, 0, 0, TAU);
            ctx.stroke();
        });
        ctx.setLineDash([]);

        ctx.globalCompositeOperation = "lighter";

        // Contour slices: front half bright, back half faint
        const leftEdge = [];
        const rightEdge = [];
        const armEdges = { "-1": [[], []], "1": [[], []] };
        // Vertical lines (body meridians, hair strands) keyed by part + point index,
        // collected from every slice so the slices read as one 3D wireframe.
        const verticals = new Map();
        const scanY = ((now / 3200) % 1.2) - 0.1;

        slices.forEach(sl => {
            const n = sl.part === "arm" ? 12 : 40;
            const yScreen = baseY - sl.y * S * breathe;
            const scan = Math.max(0, 1 - Math.abs(sl.y - scanY) / 0.04);
            const front = [];
            const back = [];
            let minX = Infinity, maxX = -Infinity;

            for (let i = 0; i <= n; i++) {
                const t = sl.from + (sl.to - sl.from) * (i / n);
                let rx = sl.rx, rz = sl.rz;
                if (sl.sway > 0) {
                    const ripple = 1 + 0.05 * sl.sway * Math.sin(t * 5 + now / 500);
                    rx *= ripple;
                    rz *= ripple;
                }
                const [x, depth] = rotate(sl.cx + rx * Math.cos(t), sl.cz + rz * Math.sin(t), cos, sin);
                const sx = cx + x * S;
                (depth >= 0 ? front : back).push([sx, yScreen, i]);
                const every = sl.part === "body" ? 5 : sl.part === "hair" ? 4 : 0;
                if (every && i % every === 0 && i < n) {
                    const key = sl.part + i;
                    if (!verticals.has(key)) verticals.set(key, []);
                    verticals.get(key).push([sx, yScreen, depth >= 0]);
                }
                if (sl.part === "body") {
                    if (sx < minX) minX = sx;
                    if (sx > maxX) maxX = sx;
                }
            }
            if (sl.part === "body") {
                leftEdge.push([minX, yScreen]);
                rightEdge.push([maxX, yScreen]);
            }
            if (sl.part === "arm") {
                let aMin = Infinity, aMax = -Infinity;
                front.concat(back).forEach(([x]) => { aMin = Math.min(aMin, x); aMax = Math.max(aMax, x); });
                armEdges[sl.side][0].push([aMin, yScreen]);
                armEdges[sl.side][1].push([aMax, yScreen]);
            }

            const base = sl.part === "hair" ? 0.85 : sl.part === "arm" ? 0.8 : 0.45;
            strokeRun(back, `rgba(${GOLD},${0.1 * boost * flicker})`, 0.8 * dpr);
            strokeRun(front, `rgba(${GOLD},${Math.min(1, (base + scan * 0.5) * boost * flicker)})`, 1 * dpr);
        });

        // Wireframe verticals: front parts only, fainter than the slices
        verticals.forEach((line, key) => {
            const hair = key.startsWith("hair");
            ctx.strokeStyle = `rgba(${GOLD},${(hair ? 0.6 : 0.28) * boost * flicker})`;
            ctx.lineWidth = 0.8 * dpr;
            ctx.beginPath();
            let drawing = false;
            line.forEach(([x, y, visible]) => {
                if (visible) {
                    if (drawing) ctx.lineTo(x, y); else ctx.moveTo(x, y);
                    drawing = true;
                } else {
                    drawing = false;
                }
            });
            ctx.stroke();
        });

        // Arm outlines
        Object.values(armEdges).forEach(edges => edges.forEach(edge => {
            ctx.strokeStyle = `rgba(255,215,140,${0.7 * flicker})`;
            ctx.lineWidth = 0.9 * dpr;
            strokePolyline(edge);
        }));

        // Crisp silhouette: the outermost points of every slice, joined top to bottom
        [leftEdge, rightEdge].forEach(edge => {
            ctx.strokeStyle = `rgba(${AMBER},${0.35 * boost})`;
            ctx.lineWidth = 3 * dpr;
            strokePolyline(edge);
            ctx.strokeStyle = `rgba(255,225,160,${0.85 * flicker})`;
            ctx.lineWidth = 1 * dpr;
            strokePolyline(edge);
        });

        ctx.globalCompositeOperation = "source-over";
    }

    /** Strokes consecutive points (by index) as connected lines. */
    function strokeRun(points, style, width) {
        if (points.length < 2) return;
        ctx.strokeStyle = style;
        ctx.lineWidth = width;
        ctx.beginPath();
        for (let i = 0; i < points.length; i++) {
            const [x, y, idx] = points[i];
            if (i === 0 || idx !== points[i - 1][2] + 1) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        }
        ctx.stroke();
    }

    function strokePolyline(points) {
        if (points.length < 2) return;
        ctx.beginPath();
        ctx.moveTo(points[0][0], points[0][1]);
        for (let i = 1; i < points.length; i++) ctx.lineTo(points[i][0], points[i][1]);
        ctx.stroke();
    }

    // ---- Loop ----

    let last = 0;
    function frame(now) {
        requestAnimationFrame(frame);
        if (now - last < FRAME_MS || document.hidden) return;
        const dt = Math.min(0.1, (now - last) / 1000);
        last = now;
        const speed = mode === "processing" ? 2.2 : 1;
        // Turntable motion: slow while facing front or back (readable silhouette),
        // quick through the side views.
        const side = Math.sin(angle) ** 2;
        angle = (angle + SPIN_PER_SECOND * speed * (0.35 + 2.2 * side) * dt) % TAU;
        draw(now);
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
