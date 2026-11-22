// =========================
// RUBY HOLOGRAPHIC GLOBE
// A rotating dot-matrix Earth drawn on a canvas (no libraries).
// Land dots come from globe-data.js (Natural Earth, public domain).
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

    // ---- Graticule (grid lines every 30 degrees) as point lists ----

    const gridLines = [];
    for (let lon = -180; lon < 180; lon += 30) {
        const line = [];
        for (let lat = -90; lat <= 90; lat += 3) line.push([lat * RAD, lon * RAD]);
        gridLines.push(line);
    }
    for (let lat = -60; lat <= 60; lat += 30) {
        const line = [];
        for (let lon = -180; lon <= 180; lon += 3) line.push([lat * RAD, lon * RAD]);
        gridLines.push(line);
    }

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

    /** Orthographic projection; returns [x, y, depth] with depth > 0 facing the viewer. */
    function project(lat, lon, cLat, cLon) {
        const dLon = lon - cLon;
        const x = Math.cos(lat) * Math.sin(dLon);
        const y = Math.sin(lat);
        const z = Math.cos(lat) * Math.cos(dLon);
        const y2 = y * Math.cos(cLat) - z * Math.sin(cLat);
        const z2 = y * Math.sin(cLat) + z * Math.cos(cLat);
        return [x, y2, z2];
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

    // ---- Drawing ----

    function draw(now) {
        const w = canvas.width;
        const cx = w / 2;
        const cy = w / 2;
        const R = w * 0.40;
        const cLat = centerLat * RAD;
        const cLon = centerLon * RAD;

        ctx.clearRect(0, 0, w, w);

        // Atmosphere glow and faint sphere body
        const glow = ctx.createRadialGradient(cx, cy, R * 0.85, cx, cy, R * 1.25);
        glow.addColorStop(0, "rgba(255,180,0,0.18)");
        glow.addColorStop(1, "rgba(255,180,0,0)");
        ctx.fillStyle = glow;
        ctx.fillRect(0, 0, w, w);

        const body = ctx.createRadialGradient(cx - R * 0.3, cy - R * 0.3, R * 0.1, cx, cy, R);
        body.addColorStop(0, "rgba(255,120,0,0.10)");
        body.addColorStop(1, "rgba(255,59,31,0.03)");
        ctx.fillStyle = body;
        ctx.beginPath();
        ctx.arc(cx, cy, R, 0, Math.PI * 2);
        ctx.fill();

        // Grid lines (front side only)
        ctx.lineWidth = 0.6 * dpr;
        ctx.strokeStyle = "rgba(255,180,0,0.16)";
        gridLines.forEach(line => {
            ctx.beginPath();
            let drawing = false;
            line.forEach(([lat, lon]) => {
                const [x, y, z] = project(lat, lon, cLat, cLon);
                if (z > 0) {
                    const sx = cx + x * R;
                    const sy = cy - y * R;
                    if (drawing) ctx.lineTo(sx, sy); else ctx.moveTo(sx, sy);
                    drawing = true;
                } else {
                    drawing = false;
                }
            });
            ctx.stroke();
        });

        // Land dots: bright in front, faint "see-through" behind
        const dotSize = 1.6 * dpr;
        for (let i = 0; i < dots.length; i++) {
            const [x, y, z] = project(dots[i][0], dots[i][1], cLat, cLon);
            const sx = cx + x * R - dotSize / 2;
            const sy = cy - y * R - dotSize / 2;
            if (z > 0) {
                ctx.fillStyle = `rgba(255,${150 + Math.round(z * 60)},0,${0.35 + z * 0.65})`;
                ctx.fillRect(sx, sy, dotSize, dotSize);
            } else {
                ctx.fillStyle = "rgba(255,90,30,0.07)";
                ctx.fillRect(sx, sy, dotSize * 0.7, dotSize * 0.7);
            }
        }

        // Rim
        ctx.strokeStyle = "rgba(255,180,0,0.55)";
        ctx.lineWidth = 1.2 * dpr;
        ctx.beginPath();
        ctx.arc(cx, cy, R, 0, Math.PI * 2);
        ctx.stroke();

        // Scanning band sweeping down the globe
        const scanY = cy - R + ((now / 25) % (2 * R + 40 * dpr)) - 20 * dpr;
        ctx.save();
        ctx.beginPath();
        ctx.arc(cx, cy, R, 0, Math.PI * 2);
        ctx.clip();
        const band = ctx.createLinearGradient(0, scanY - 14 * dpr, 0, scanY + 14 * dpr);
        band.addColorStop(0, "rgba(255,180,0,0)");
        band.addColorStop(0.5, "rgba(255,200,80,0.18)");
        band.addColorStop(1, "rgba(255,180,0,0)");
        ctx.fillStyle = band;
        ctx.fillRect(cx - R, scanY - 14 * dpr, 2 * R, 28 * dpr);
        ctx.restore();

        // Marker: pulsing beacon with a beam
        if (marker) {
            const [x, y, z] = project(marker.lat * RAD, marker.lon * RAD, cLat, cLon);
            if (z > 0) {
                const mx = cx + x * R;
                const my = cy - y * R;
                const pulse = (now % 1600) / 1600;

                ctx.strokeStyle = `rgba(255,59,31,${1 - pulse})`;
                ctx.lineWidth = 1.5 * dpr;
                ctx.beginPath();
                ctx.arc(mx, my, (3 + pulse * 12) * dpr, 0, Math.PI * 2);
                ctx.stroke();

                const beam = ctx.createLinearGradient(mx, my, mx, my - 26 * dpr);
                beam.addColorStop(0, "rgba(255,59,31,0.9)");
                beam.addColorStop(1, "rgba(255,59,31,0)");
                ctx.strokeStyle = beam;
                ctx.lineWidth = 2 * dpr;
                ctx.beginPath();
                ctx.moveTo(mx, my);
                ctx.lineTo(mx, my - 26 * dpr);
                ctx.stroke();

                ctx.fillStyle = "#ff3b1f";
                ctx.shadowColor = "#ff3b1f";
                ctx.shadowBlur = 10 * dpr;
                ctx.beginPath();
                ctx.arc(mx, my, 2.8 * dpr, 0, Math.PI * 2);
                ctx.fill();
                ctx.shadowBlur = 0;

                ctx.fillStyle = "rgba(255,255,255,0.85)";
                ctx.font = `${9 * dpr}px Orbitron, sans-serif`;
                ctx.textAlign = "center";
                ctx.fillText(marker.label.toUpperCase(), mx, my - 30 * dpr);
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
