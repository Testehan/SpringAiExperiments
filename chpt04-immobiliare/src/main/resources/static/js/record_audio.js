
let mediaRecorder;
let audioChunks = [];

$(document).ready(function () {
    // Request access to the user's microphone
    navigator.mediaDevices.getUserMedia({ audio: true })
        .then(stream => {
            mediaRecorder = new MediaRecorder(stream);

            // When data is available (while recording)
            mediaRecorder.addEventListener("dataavailable", event => {
                audioChunks.push(event.data);
            });

            // When recording stops
            mediaRecorder.addEventListener("stop", () => {
                const audioBlob = new Blob(audioChunks, { type: 'audio/wav' });
                const audioURL = URL.createObjectURL(audioBlob);

                // Attach audio file to the form as a hidden file input
                const audioInput =  $("#audioFile");
                const file = new File([audioBlob], "voiceRecording.wav");
                const dataTransfer = new DataTransfer();
                dataTransfer.items.add(file);
                audioInput[0].files = dataTransfer.files;

                audioChunks = [];  // Clear the chunks
            });
        });

    // Start and stop recording on button click
    $("#recordVoiceButton").on('click', function() {
        if (mediaRecorder.state === "inactive") {
            mediaRecorder.start();
            this.textContent = "Recording...";
        } else {
            mediaRecorder.stop();
            this.textContent = "Record Voice";
        }
    });
});
