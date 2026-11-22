// RUBY service worker: makes the app installable and lets the interface
// load without a network. Always tries the network first, so a new
// version is picked up immediately; the cache is only a fallback.
// API calls (/api/...) are never cached: answers must be live.

const CACHE = "ruby-v4";

const SHELL = [
    "/", "/index.html", "/style.css",
    "/sfx.js", "/globe-data.js", "/globe.js", "/hologram.js", "/hud.js", "/voice.js", "/widgets.js", "/app.js",
    "/manifest.webmanifest", "/icons/icon-192.png", "/icons/icon-512.png"
];

self.addEventListener("install", event => {
    event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(SHELL)));
    self.skipWaiting();
});

self.addEventListener("activate", event => {
    event.waitUntil(
        caches.keys()
            .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener("fetch", event => {
    const url = new URL(event.request.url);
    if (event.request.method !== "GET" || url.origin !== location.origin || url.pathname.startsWith("/api/")) {
        return;
    }
    event.respondWith(
        fetch(event.request)
            .then(response => {
                if (response.ok) {
                    const copy = response.clone();
                    caches.open(CACHE).then(cache => cache.put(event.request, copy));
                }
                return response;
            })
            .catch(() => caches.match(event.request))
    );
});
