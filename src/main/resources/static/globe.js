// =========================
// RUBY HOLOGRAPHIC GLOBE
// A glowing dot-matrix planet with city nodes, light-trail arcs between
// cities and a tilted orbit ring, in RUBY's gold/red theme.
// Drawn on a canvas with no libraries. Land dots come from globe-data.js
// (Natural Earth, public domain).
// =========================

const RubyGlobe = (() => {

    const canvas = document.getElementById("globeCanvas");
    if (!canvas || typeof GLOBE_LAND === "undefined") {
        return { focus() {}, setHome() {} };
    }

    const ctx = canvas.getContext("2d");
    const latOut = document.getElementById("globeLat");
    const lonOut = document.getElementById("globeLon");
    const targetOut = document.getElementById("globeTarget");
    const locateBtn = document.getElementById("locateBtn");

    const RAD = Math.PI / 180;
    const SPIN_PER_SECOND = 4;          // degrees of auto-rotation per second
    const RESUME_SPIN_AFTER_MS = 20000; // after focusing on a place
    const FRAME_MS = 33;                // ~30 fps keeps CPU use low
    const RADIUS = 0.34;                // globe radius as a share of canvas width
    const MAX_ARCS = 10;

    // Theme colours (match --gold / --red / --orange in style.css)
    const GOLD = [255, 180, 0];
    const RED = [255, 59, 31];
    const ORANGE = [255, 107, 0];
    const rgba = (c, a) => `rgba(${c[0]},${c[1]},${c[2]},${a})`;

    // ---- Land dots as [latitude, longitude] in radians ----

    const dots = [];
    GLOBE_LAND.forEach(([lat, count, runs]) => {
        const step = 360 / count;
        for (let r = 0; r < runs.length; r += 2) {
            for (let i = runs[r]; i < runs[r] + runs[r + 1]; i++) {
                dots.push([lat * RAD, (-180 + i * step) * RAD]);
            }
        }
    });

    // ---- Cities that light up and exchange light trails ----

    const CITIES = [
        ["New Delhi", 28.61, 77.21], ["Mumbai", 19.08, 72.88], ["Bengaluru", 12.97, 77.59],
        ["Dubai", 25.2, 55.27], ["London", 51.51, -0.13], ["Paris", 48.86, 2.35],
        ["Moscow", 55.76, 37.62], ["New York", 40.71, -74.0], ["Toronto", 43.65, -79.38],
        ["San Francisco", 37.77, -122.42], ["Los Angeles", 34.05, -118.24], ["São Paulo", -23.55, -46.63],
        ["Cape Town", -33.92, 18.42], ["Nairobi", -1.29, 36.82], ["Cairo", 30.04, 31.24],
        ["Singapore", 1.35, 103.82], ["Tokyo", 35.68, 139.69], ["Beijing", 39.9, 116.4],
        ["Sydney", -33.87, 151.21], ["Jakarta", -6.2, 106.85]
    ].map(([name, lat, lon]) => ({ name, lat: lat * RAD, lon: lon * RAD }));

    // ---- View state: which longitude/latitude faces the viewer ----

    let centerLon = 77;   // start over India; replaced by setHome()
    let centerLat = 20;
    let targetLon = null;
    let targetLat = null;
    let spinning = true;
    let resumeTimer = null;
    let marker = null;    // { lat, lon, label } in degrees

    let size = 0;
    let dpr = 1;

    function resize() {
        dpr = Math.min(window.devicePixelRatio || 1, 2);
        size = canvas.clientWidth;
        canvas.width = Math.round(size * dpr);
        canvas.height = Math.round(size * dpr);
    }

    resize();
    if (window.ResizeObserver) new ResizeObserver(resize).observe(canvas);

    /**
     * Orthographic projection of a point at the given height above the
     * surface (1 = surface). Returns [x, y, depth]; depth > 0 faces the viewer.
     */
    function project(lat, lon, cLat, cLon, height = 1) {
        const dLon = lon - cLon;
        const x = Math.cos(lat) * Math.sin(dLon) * height;
        const y = Math.sin(lat) * height;
        const z = Math.cos(lat) * Math.cos(dLon) * height;
        const y2 = y * Math.cos(cLat) - z * Math.sin(cLat);
        const z2 = y * Math.sin(cLat) + z * Math.cos(cLat);
        return [x, y2, z2];
    }

    /** A point is hidden only if it is behind the planet AND inside its silhouette. */
    function visible(x, y, z) {
        return z >= 0 || x * x + y * y > 1;
    }

    /** Shortest signed angle from a to b, in degrees. */
    function angleDelta(a, b) {
        return ((b - a + 540) % 360) - 180;
    }

    function formatCoordinate(value, positive, negative) {
        return Math.abs(value).toFixed(4) + "° " + (value >= 0 ? positive : negative);
    }

    function showReadout(lat, lon, label) {
        if (latOut) latOut.textContent = formatCoordinate(lat, "N", "S");
        if (lonOut) lonOut.textContent = formatCoordinate(lon, "E", "W");
        if (targetOut) targetOut.textContent = (label || "--").toUpperCase();
        [latOut, lonOut, targetOut].forEach(node => {
            if (!node) return;
            node.classList.remove("flash");
            void node.offsetWidth;
            node.classList.add("flash");
        });
    }

    // ---- Light-trail arcs between cities ----

    const arcs = [];

    function unitVector(lat, lon) {
        return [Math.cos(lat) * Math.cos(lon), Math.cos(lat) * Math.sin(lon), Math.sin(lat)];
    }

    function toLatLon(v) {
        return [Math.asin(Math.max(-1, Math.min(1, v[2]))), Math.atan2(v[1], v[0])];
    }

    /** Great-circle path from a to b, lifted above the surface in the middle. */
    function buildPath(a, b) {
        const va = unitVector(a.lat, a.lon);
        const vb = unitVector(b.lat, b.lon);
        const dot = Math.max(-1, Math.min(1, va[0] * vb[0] + va[1] * vb[1] + va[2] * vb[2]));
        const omega = Math.acos(dot);
        const lift = 0.18 + 0.38 * (omega / Math.PI);
        const steps = 48;
        const path = [];
        for (let i = 0; i <= steps; i++) {
            const t = i / steps;
            const s1 = Math.sin((1 - t) * omega) / Math.sin(omega);
            const s2 = Math.sin(t * omega) / Math.sin(omega);
            const v = [va[0] * s1 + vb[0] * s2, va[1] * s1 + vb[1] * s2, va[2] * s1 + vb[2] * s2];
            const [lat, lon] = toLatLon(v);
            path.push([lat, lon, 1 + lift * Math.sin(Math.PI * t)]);
        }
        return path;
    }

    function spawnArc() {
        const from = CITIES[Math.floor(Math.random() * CITIES.length)];
        let to = from;
        while (to === from) to = CITIES[Math.floor(Math.random() * CITIES.length)];
        arcs.push({
            path: buildPath(from, to),
            to,
            color: Math.random() < 0.55 ? GOLD : RED,
            progress: 0,                         // head position 0..1
            speed: 0.35 + Math.random() * 0.3,   // path lengths per second
            fade: 1
        });
    }

    function updateArcs(dt) {
        while (arcs.length < MAX_ARCS) spawnArc();
        for (let i = arcs.length - 1; i >= 0; i--) {
            const arc = arcs[i];
            if (arc.progress < 1.35) {
                arc.progress += arc.speed * dt;
            } else {
                arc.fade -= dt * 1.5;
                if (arc.fade <= 0) arcs.splice(i, 1);
            }
        }
    }

    // ---- Drawing helpers ----

    function glowDot(x, y, radius, color, alpha) {
        const g = ctx.createRadialGradient(x, y, 0, x, y, radius);
        g.addColorStop(0, `rgba(255,255,255,${alpha})`);
        g.addColorStop(0.35, rgba(color, alpha * 0.8));
        g.addColorStop(1, rgba(color, 0));
        ctx.fillStyle = g;
        ctx.fillRect(x - radius, y - radius, radius * 2, radius * 2);
    }

    /** Tilted elliptical orbit; draws only the back or front half. */
    function drawOrbit(cx, cy, R, now, front) {
        const rx = R * 1.32;
        const ry = R * 0.38;
        const tilt = -0.32;
        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(tilt);
        ctx.strokeStyle = rgba(GOLD, front ? 0.55 : 0.18);
        ctx.lineWidth = 1 * dpr;
        ctx.beginPath();
        ctx.ellipse(0, 0, rx, ry, 0, front ? 0 : Math.PI, front ? Math.PI : Math.PI * 2);
        ctx.stroke();

        // Satellite travelling along the orbit
        const angle = (now / 4000) % (Math.PI * 2);
        const inFront = Math.sin(angle) > 0;
        if (inFront === front) {
            glowDot(Math.cos(angle) * rx, Math.sin(angle) * ry, 7 * dpr, ORANGE, front ? 1 : 0.35);
        }
        ctx.restore();
    }

    // ---- Main draw ----

    function draw(now) {
        const w = canvas.width;
        const cx = w / 2;
        const cy = w / 2;
        const R = w * RADIUS;
        const cLat = centerLat * RAD;
        const cLon = centerLon * RAD;

        ctx.clearRect(0, 0, w, w);
        ctx.globalCompositeOperation = "source-over";

        // Outer halo
        const halo = ctx.createRadialGradient(cx, cy, R * 0.9, cx, cy, R * 1.45);
        halo.addColorStop(0, rgba(ORANGE, 0.35));
        halo.addColorStop(0.4, rgba(RED, 0.12));
        halo.addColorStop(1, rgba(RED, 0));
        ctx.fillStyle = halo;
        ctx.fillRect(0, 0, w, w);

        drawOrbit(cx, cy, R, now, false);

        // Planet body: dark ember core, lit from the upper left
        const body = ctx.createRadialGradient(cx - R * 0.35, cy - R * 0.4, R * 0.05, cx, cy, R);
        body.addColorStop(0, "rgba(90,30,0,0.95)");
        body.addColorStop(0.55, "rgba(35,10,0,0.97)");
        body.addColorStop(1, "rgba(18,4,0,0.98)");
        ctx.fillStyle = body;
        ctx.beginPath();
        ctx.arc(cx, cy, R, 0, Math.PI * 2);
        ctx.fill();

        // Everything that glows is drawn additively
        ctx.globalCompositeOperation = "lighter";

        // Land dots: size and brightness follow depth (bigger and whiter in front)
        for (let i = 0; i < dots.length; i++) {
            const [x, y, z] = project(dots[i][0], dots[i][1], cLat, cLon);
            if (z <= 0) continue;
            const sx = cx + x * R;
            const sy = cy - y * R;
            // small dots with dark gaps keep the "dot matrix" look
            const s = (0.6 + z * 0.85) * dpr;
            const light = Math.max(0, z * 0.7 + (-x + y) * 0.25);
            ctx.fillStyle = `rgba(255,${Math.round(140 + light * 100)},${Math.round(light * 110)},${0.2 + z * 0.6})`;
            ctx.fillRect(sx - s / 2, sy - s / 2, s, s);
            // soft glow around each dot
            ctx.fillStyle = rgba(GOLD, 0.025 + z * 0.035);
            ctx.fillRect(sx - s * 1.5, sy - s * 1.5, s * 3, s * 3);
        }

        // Rim light (bright edge like a hologram)
        const rim = ctx.createRadialGradient(cx, cy, R * 0.72, cx, cy, R * 1.02);
        rim.addColorStop(0, rgba(GOLD, 0));
        rim.addColorStop(0.85, rgba(ORANGE, 0.22));
        rim.addColorStop(1, rgba(GOLD, 0.55));
        ctx.fillStyle = rim;
        ctx.beginPath();
        ctx.arc(cx, cy, R * 1.02, 0, Math.PI * 2);
        ctx.fill();

        // City nodes
        CITIES.forEach(city => {
            const [x, y, z] = project(city.lat, city.lon, cLat, cLon);
            if (z > 0.05) glowDot(cx + x * R, cy - y * R, (3 + z * 4) * dpr, GOLD, 0.5 + z * 0.5);
        });

        // Light-trail arcs: comet head with a fading tail
        arcs.forEach(arc => {
            const n = arc.path.length - 1;
            const head = Math.min(arc.progress, 1);
            const tail = Math.max(0, arc.progress - 0.6);
            const from = Math.floor(tail * n);
            const to = Math.floor(head * n);
            ctx.lineCap = "round";
            for (let i = from; i < to; i++) {
                const p1 = project(arc.path[i][0], arc.path[i][1], cLat, cLon, arc.path[i][2]);
                const p2 = project(arc.path[i + 1][0], arc.path[i + 1][1], cLat, cLon, arc.path[i + 1][2]);
                if (!visible(...p1) || !visible(...p2)) continue;
                const t = (i - from) / Math.max(1, to - from);     // 0 at tail, 1 at head
                ctx.strokeStyle = rgba(arc.color, t * 0.9 * arc.fade);
                ctx.lineWidth = (0.7 + t * 1.9) * dpr;
                ctx.beginPath();
                ctx.moveTo(cx + p1[0] * R, cy - p1[1] * R);
                ctx.lineTo(cx + p2[0] * R, cy - p2[1] * R);
                ctx.stroke();
            }
            // comet head
            if (arc.progress < 1) {
                const p = arc.path[to];
                const [x, y, z] = project(p[0], p[1], cLat, cLon, p[2]);
                if (visible(x, y, z)) glowDot(cx + x * R, cy - y * R, 6 * dpr, arc.color, 1);
            }
            // impact pulse when it lands
            if (arc.progress >= 1 && arc.progress < 1.35) {
                const [x, y, z] = project(arc.to.lat, arc.to.lon, cLat, cLon);
                if (z > 0) {
                    const k = (arc.progress - 1) / 0.35;
                    ctx.strokeStyle = rgba(arc.color, 1 - k);
                    ctx.lineWidth = 1.2 * dpr;
                    ctx.beginPath();
                    ctx.arc(cx + x * R, cy - y * R, (2 + k * 10) * dpr, 0, Math.PI * 2);
                    ctx.stroke();
                }
            }
        });

        ctx.globalCompositeOperation = "source-over";

        // Thin bright rim line
        ctx.strokeStyle = rgba(GOLD, 0.7);
        ctx.lineWidth = 1 * dpr;
        ctx.beginPath();
        ctx.arc(cx, cy, R, 0, Math.PI * 2);
        ctx.stroke();

        drawOrbit(cx, cy, R, now, true);

        // Location marker: pulsing beacon with a beam and label
        if (marker) {
            const [x, y, z] = project(marker.lat * RAD, marker.lon * RAD, cLat, cLon);
            if (z > 0) {
                const mx = cx + x * R;
                const my = cy - y * R;
                const pulse = (now % 1600) / 1600;

                ctx.strokeStyle = rgba(RED, 1 - pulse);
                ctx.lineWidth = 1.5 * dpr;
                ctx.beginPath();
                ctx.arc(mx, my, (3 + pulse * 13) * dpr, 0, Math.PI * 2);
                ctx.stroke();

                const beam = ctx.createLinearGradient(mx, my, mx, my - 30 * dpr);
                beam.addColorStop(0, rgba(RED, 0.95));
                beam.addColorStop(1, rgba(RED, 0));
                ctx.strokeStyle = beam;
                ctx.lineWidth = 2 * dpr;
                ctx.beginPath();
                ctx.moveTo(mx, my);
                ctx.lineTo(mx, my - 30 * dpr);
                ctx.stroke();

                ctx.globalCompositeOperation = "lighter";
                glowDot(mx, my, 9 * dpr, RED, 1);
                ctx.globalCompositeOperation = "source-over";

                ctx.fillStyle = "rgba(255,255,255,0.9)";
                ctx.font = `${9 * dpr}px Orbitron, sans-serif`;
                ctx.textAlign = "center";
                ctx.fillText(marker.label.toUpperCase(), mx, my - 34 * dpr);
            }
        }
    }

    // ---- Animation loop ----

    let last = 0;

    function frame(now) {
        requestAnimationFrame(frame);
        if (now - last < FRAME_MS || document.hidden || size === 0) return;
        const dt = Math.min(0.1, (now - last) / 1000);
        last = now;

        if (targetLon !== null) {
            // Ease towards the focused place along the shortest path.
            const dLon = angleDelta(centerLon, targetLon);
            const dLat = targetLat - centerLat;
            centerLon += dLon * 0.08;
            centerLat += dLat * 0.08;
            if (Math.abs(dLon) < 0.05 && Math.abs(dLat) < 0.05) {
                centerLon = targetLon;
                centerLat = targetLat;
                targetLon = targetLat = null;
            }
        } else if (spinning) {
            centerLon = ((centerLon + SPIN_PER_SECOND * dt + 540) % 360) - 180;
            centerLat += (15 - centerLat) * 0.01; // drift back to a gentle tilt
        }

        updateArcs(dt);
        draw(now);
    }

    requestAnimationFrame(frame);

    // ---- Public API ----

    /** Turn the globe to a place and mark it (e.g. "weather in London"). */
    function focus(lat, lon, label) {
        if (typeof lat !== "number" || typeof lon !== "number") return;
        marker = { lat, lon, label: label || "" };
        targetLon = lon;
        targetLat = Math.max(-55, Math.min(55, lat));
        spinning = false;
        showReadout(lat, lon, label);
        clearTimeout(resumeTimer);
        resumeTimer = setTimeout(() => { spinning = true; }, RESUME_SPIN_AFTER_MS);
    }

    /** The default city from the weather panel; shown until something else is focused. */
    let homeSet = false;
    function setHome(lat, lon, label) {
        if (homeSet && marker) return;
        homeSet = true;
        focus(lat, lon, label);
    }

    if (locateBtn) {
        if (!navigator.geolocation) {
            locateBtn.disabled = true;
        } else {
            locateBtn.addEventListener("click", () => {
                locateBtn.textContent = "LOCATING…";
                navigator.geolocation.getCurrentPosition(
                    pos => {
                        locateBtn.textContent = "LOCATE";
                        focus(pos.coords.latitude, pos.coords.longitude, "You");
                    },
                    () => {
                        locateBtn.textContent = "LOCATE";
                        if (targetOut) targetOut.textContent = "ACCESS DENIED";
                    },
                    { enableHighAccuracy: false, timeout: 10000, maximumAge: 600000 }
                );
            });
        }
    }

    return { focus, setHome };
})();
