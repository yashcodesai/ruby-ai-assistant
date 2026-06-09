const btn = document.getElementById("listenBtn");
const response = document.getElementById("response");
const history = document.getElementById("history");

const core =
    document.querySelector(".core");

const coreStatus =
    document.getElementById("coreStatus");

let availableVoices = [];

let recognition;

let isListening = false;

speechSynthesis.onvoiceschanged = () => {
    availableVoices =
        speechSynthesis.getVoices();
};

btn.addEventListener(
    "click",
    startListening
);

function setState(state) {

    core.className = "core " + state;

    switch(state){

        case "listening":
            coreStatus.innerText =
                "LISTENING";
            break;

        case "processing":
            coreStatus.innerText =
                "THINKING";
            break;

        case "speaking":
            coreStatus.innerText =
                "SPEAKING";
            break;

        default:
            coreStatus.innerText =
                "STANDBY";
    }
}
function addHistory(sender, message) {

    const now = new Date();

    const time =
        now.toLocaleTimeString();

    const entry =
        document.createElement("p");

    entry.innerHTML =
        `[${time}] <b>${sender}</b> > ${message}`;

    history.appendChild(entry);

    while (
        history.children.length > 20
        ) {
        history.removeChild(
            history.firstChild
        );
    }

    history.scrollTop =
        history.scrollHeight;
}

function startListening() {

    if(isListening) return;

    isListening = true;

    recognition =
        new webkitSpeechRecognition();

    recognition.lang = "en-US";

    recognition.interimResults = false;

    recognition.maxAlternatives = 1;

    response.innerText =
        "🎤 Listening...";

    setState("listening");

    recognition.start();

    recognition.onresult =
        async (event) => {

            const text =
                event.results[0][0].transcript;

            response.innerText =
                "You said: " + text;

            addHistory(
                "USER",
                text
            );

            setState("processing");

            try {

                const res =
                    await fetch(
                        "/api/chat",
                        {
                            method:"POST",
                            headers:{
                                "Content-Type":
                                    "application/json"
                            },
                            body:JSON.stringify({
                                message:text
                            })
                        }
                    );

                const data =
                    await res.json();
                if(data.action){

                    switch(data.action){

                        case "OPEN_GOOGLE":

                            window.open(
                                "https://google.com",
                                "_blank"
                            );
                            break;

                        case "OPEN_YOUTUBE":

                            window.open(
                                "https://youtube.com",
                                "_blank"
                            );
                            break;

                        case "OPEN_GITHUB":

                            window.open(
                                "https://github.com",
                                "_blank"
                            );
                            break;

                        case "OPEN_CHATGPT":

                            window.open(
                                "https://chatgpt.com",
                                "_blank"
                            );
                            break;
                    }
                }

                response.innerText =
                    "RUBY: " +
                    data.response;

                addHistory(
                    "RUBY",
                    data.response
                );

                speak(data.response);

            } catch(error){

                console.error(error);

                response.innerText =
                    "Failed to contact RUBY backend";

                setState("listening");
            }
        };

    recognition.onerror =
        (event) => {

            console.log(
                "Recognition Error:",
                event.error
            );

            isListening = false;

            response.innerText =
                "Microphone error: " +
                event.error;

            setState("standby");
        };

    recognition.onend = () => {

        isListening = false;
    };
}

function speak(text) {

    speechSynthesis.cancel();

    setState("speaking");

    const speech =
        new SpeechSynthesisUtterance(
            text
        );

    const preferredVoice =

        availableVoices.find(
            v => v.name === "Moira"
        ) ||

        availableVoices.find(
            v => v.name === "Samantha"
        ) ||

        availableVoices.find(
            v =>
                v.name ===
                "Google UK English Female"
        );

    if(preferredVoice){

        speech.voice =
            preferredVoice;
    }

    speech.rate = 1;

    speech.pitch = 1;

    speech.volume = 1;

    speech.lang = "en-US";

    speech.onstart = () => {

        response.innerText =
            "🔊 RUBY is speaking...";

        setState("speaking");
    };

    speech.onend = () => {

        response.innerText =
            "🎤 Listening for next command...";

        setState("listening");

        setTimeout(() => {

            startListening();

        }, 500);
    };

    speechSynthesis.speak(
        speech
    );
}