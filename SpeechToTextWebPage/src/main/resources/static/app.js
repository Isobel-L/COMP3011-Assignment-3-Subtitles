const statusText = document.getElementById("statusText");
const checkStatusBtn = document.getElementById("checkStatusBtn");

const startBtn = document.getElementById("startBtn");
const stopBtn = document.getElementById("stopBtn");
const recordingState = document.getElementById("recordingState");

let mediaRecorder = null;
let audioChunks = [];

checkStatusBtn.addEventListener("click", async () => {
    try {
        const response = await fetch("/api/v1/record/status");
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();
        statusText.textContent = data.message;
    } catch (error) {
        statusText.textContent = `Status check failed: ${error.message}`;
    }
});

startBtn.addEventListener("click", async () => {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

        audioChunks = [];
        mediaRecorder = new MediaRecorder(stream);

        mediaRecorder.ondataavailable = event => {
            if (event.data.size > 0) {
                audioChunks.push(event.data);
            }
        };

        mediaRecorder.onstart = () => {
            recordingState.textContent = "Recording...";
            startBtn.disabled = true;
            stopBtn.disabled = false;
        };

        mediaRecorder.onstop = async () => {
            recordingState.textContent = "Stopped";
            startBtn.disabled = false;
            stopBtn.disabled = true;

            const audioBlob = new Blob(audioChunks, { type: "audio/webm" });
            await uploadAudio(audioBlob);

            stream.getTracks().forEach(track => track.stop());
        };

        mediaRecorder.start();
    } catch (error) {
        recordingState.textContent = `Could not start recording: ${error.message}`;
    }
});

stopBtn.addEventListener("click", () => {
    if (mediaRecorder && mediaRecorder.state === "recording") {
        mediaRecorder.stop();
    }
});

async function uploadAudio(audioBlob) {
    const formData = new FormData();
    formData.append("file", audioBlob, "recording.webm");

    try {
        const response = await fetch("/api/v1/record/upload", {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();
		document.getElementById("transcriptText").textContent = data.transcript;
		recordingState.textContent = "Transcription complete";		
    } catch (error) {
        recordingState.textContent = `Upload failed: ${error.message}`;
    }
}