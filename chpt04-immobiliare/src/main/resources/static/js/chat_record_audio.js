
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

                 $("#sendMessageButton").click();   // send request once recording stops
                 $('#message').attr('required', true);
            });
        });

    // Start and stop recording on button click
    $("#recordVoiceButton").on('click', function() {
         $('#spinner').show();
        if (mediaRecorder.state === "inactive") {
            mediaRecorder.start();
            $("#recordMicrophone").attr( { 'src' : '/images/stop-microphone.svg' } );
        } else {
            mediaRecorder.stop();
            $("#recordMicrophone").attr( { 'src' : '/images/microphone.svg' } );
            $("#message").removeAttr("required");
        }
    });
});
