let suggestionsStep = 1;
let lastAssistantMessage;
let lastHelperMessage;

let eventSource;
let lastPingTime = new Date(); // Store the last received ping time
let pingTimeout;
let isCheckingInternet = false;

let chatRequestTimeout;

let mediaRecorder;
let audioChunks = [];
let recordingTimeout;
let countdownInterval;
const MAX_RECORDING_TIME = 30; // Maximum recording time in seconds

$(document).ready(function(){
    // after voice message is transcribed, or text message is added to chat, send a request to obtain a response
    $(document).bind('htmx:load', function(evt) {
        if ((evt.target.parentNode.id === 'response-container')
            && (evt.target.children.length>0)
            && (evt.target.children[0].className.includes("userMessage"))) {
                askForResponse();
        }
    });

    $("#message").on("input", function() {
        if ($(this)[0].checkValidity()) {
            $("#sendMessageButton").show();
        } else {
            $("#sendMessageButton").hide();
        }
    });

    getNewSseId(function(newSseId) {
        console.log("Got new SSEid calling the endpoint " + newSseId.sseId);
        $("#testing-sse-id").text(newSseId.sseId);
        connectToSSE(newSseId.sseId);
    });

    setCurrentStep();

    $(document).on("click", ".suggestion-btn", function (e) {
        const suggestionText = $(this).text(); // Get button text
        $("#message").val(suggestionText); // Populate input box
        $('#spinner').show();
        $("#sendMessageButton").click();
        enableScrollTracking();
    });

    setUpAudioRecording();

});

function setUpAudioRecording(){
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
                clearTimeout(recordingTimeout); // Clear timeout if stopped manually
                clearInterval(countdownInterval);

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
}

function setOnClickForRecordAudio(){
    // Start and stop recording on button click
    $("#recordVoiceButton").on('click', function() {
         $("#audioRecProgress").show();
        if (mediaRecorder.state === "inactive") {
            mediaRecorder.start();
            $("#recordMicrophone").attr( { 'src' : '/images/stop-microphone.svg' } );

            let timeLeft = MAX_RECORDING_TIME;
            $("#audioRecProgress").text(function(_, currentText) {
                return currentText.replace(/(\d+s)/, ""); // removed old timers from element
            });
            $("#audioRecProgress").append(" " + timeLeft + "s");    // add initial timer value

            countdownInterval = setInterval(() => {
                timeLeft--;
                // Append countdown seconds to the existing text
                $("#audioRecProgress").text(function(_, currentText) {
                    return currentText.replace(/(\d+s)/, `${timeLeft}s`); // Update countdown
                });

                if (timeLeft <= 0) {
                    clearInterval(countdownInterval);
                }
            }, 1000);

            recordingTimeout = setTimeout(() => {
                if (mediaRecorder.state === "recording") {
                    mediaRecorder.stop();
                    $('#audioRecProgress').hide();
                    $("#recordMicrophone").attr('src', '/images/microphone.svg');
                    $("#message").removeAttr("required");
                }
            }, MAX_RECORDING_TIME * 1000);

        } else {
            mediaRecorder.stop();
            clearTimeout(recordingTimeout); // If stopped manually, clear timeout
            clearInterval(countdownInterval);
            $('#audioRecProgress').hide();
            $("#recordMicrophone").attr( { 'src' : '/images/microphone.svg' } );
            $("#message").removeAttr("required");
        }
    });

     $("#audioRecProgress").on('click', function() {
         if (mediaRecorder.state !== "inactive") {
            mediaRecorder.stop();
            clearTimeout(recordingTimeout); // If stopped manually, clear timeout
            clearInterval(countdownInterval);
            $('#audioRecProgress').hide();
            $("#recordMicrophone").attr( { 'src' : '/images/microphone.svg' } );
            $("#message").removeAttr("required");
         }
    });
}

function removeOnClickForRecordAudio(){
     $("#recordVoiceButton").off("click");
}

function disableChatInput(){
    $("#message").prop("disabled", true);
    $(".suggestion-btn").prop("disabled", true);
    $("#sendMessageButton").prop("disabled", true);
    $("#recordVoiceButton").prop("disabled", false);
    removeOnClickForRecordAudio();
    $('#spinner').hide();
    clearChatRequestTimeout();
}

function enableChatInput(){
    $("#message").prop("disabled", false);
    $(".suggestion-btn").prop("disabled", false);
    $("#sendMessageButton").prop("disabled", false);
    $("#recordVoiceButton").prop("disabled", false);
    setOnClickForRecordAudio();
}

function showToast(message, durationMillis, type = "info") {
    if ($(".toastify").length === 0) {
        let bgColor;
        if (type === "error") {
            bgColor = "linear-gradient(to right, #fd0713, #ff7675)";
        } else if (type === "info") {
            bgColor = "linear-gradient(to right, #7ea82b, #c6e28c)";
        } else {
            bgColor = "linear-gradient(to right, #e67e22, #f39c12)"; // Default to "warning"
        }

        Toastify({
            text: message,
            duration: durationMillis, // -1 value Keep it until dismissed
            backgroundColor: bgColor,
            gravity: "top", // Position it at the top
            position: "center", // Center it horizontally
            close: true
        }).showToast();
    }
}

function dismissToast() {
    $(".toastify").remove();
}

function reconnect() {
    showToast(TOASTIFY_RECONNECTING, -1, "warn");

    if (eventSource) {
        eventSource.close();
    }

    getNewSseId(function(newSseId) {
        console.log("Got new SSEid calling the endpoint " + newSseId.sseId);
         $("#testing-sse-id").text(newSseId.sseId);
        connectToSSE(newSseId.sseId);
    });
}

function checkInternetAndReconnect() {
    if (isCheckingInternet) return; // Prevent multiple intervals
    isCheckingInternet = true;

    let internetCheckInterval = setInterval(() => {
        if (navigator.onLine) {
            clearInterval(internetCheckInterval); // Stop checking
            isCheckingInternet = false;
            console.log("Internet restored! Reconnecting...");
            dismissToast();
            reconnect();
        } else {
            let toastText = $(".toastify").text().trim();
            if (!toastText.includes(TOASTIFY_NO_INTERNET)) {
                dismissToast();
                showToast(TOASTIFY_NO_INTERNET, -1, "error");
                disableChatInput();
            }
        }
    }, 2000);
}

function connectToSSE(sseId) {
    try {
        console.log("SSE ID:", sseId);
        const eventSourceUrl = "/api/apartments/stream/" + sseId;
        console.log("eventSourceUrl" + eventSourceUrl)
        eventSource = new EventSource(eventSourceUrl);

        eventSource.onopen = function () {
            if ($(".toastify").length !== 0) {  // means we had a warn or error toast before, so we want to announce the user that the connection is restored
                dismissToast();
                showToast(TOASTIFY_CONNECTED, 2000, "info");
            }
            enableChatInput();
        };

        eventSource.addEventListener('apartment', function(event) {
            if (event.lastEventId === sseId) {
                 // Dynamically insert the fragment into the response container
                const container = $('.responseFragmentWithApartments').last()
                const newFragment = event.data;
                $('#spinner').hide();
                clearChatRequestTimeout();
                container.append(newFragment);

                applyFavouriteButtonStylingDependingOnText($('.favouriteButton').last());

                // we need this in order to have the Contact and Favourite working when obtaining html string containing htmx from server
                htmx.process($('#response-container')[0]);

                setTimeout(() => {
                    initializeSwipers();

                    // Ensure the new slides are added before updating Swiper
                    swiperInstances.forEach(swiper => {
                        swiper.update(); // Update each swiper instance
                        swiper.pagination.update(); // Update pagination
                    });
                }, 200); // Small delay to ensure new elements exist
            }
        });

        eventSource.addEventListener('response', function(event) {
             if (event.lastEventId === sseId) {
                // Dynamically insert the fragment into the response container
                const container = $("#response-container").last()
                const newFragment = event.data;
                $('#spinner').hide();
                clearChatRequestTimeout();
                container.append(newFragment);
                addReportInaccurateResponse();
            }

        });

        eventSource.addEventListener('keep-alive', function(event) {
            if (event.data === 'ping') {
                clearTimeout(pingTimeout); // Prevent multiple timeouts
                lastPingTime = new Date();
                pingTimeout = setTimeout(() => {
                    console.warn("No ping received for 35 seconds. Reconnecting...");
                    checkInternetAndReconnect();
                }, 15000); // Wait 35 seconds before assuming the connection is lost
            }

        });

        eventSource.onerror = function() {
            console.error("Connection lost. Reconnecting...");
            showToast(TOASTIFY_DISCONNECTED, -1, "warn");
            disableChatInput();
            checkInternetAndReconnect();
        };
    } catch (error) {
        console.error("Failed to initialize SSE:", error);
        showToast(TOASTIFY_DISCONNECTED, -1, "warn");
        disableChatInput();
        checkInternetAndReconnect();
    }

}

// Function to check if last ping is older than 15 seconds
function checkPingTimeout() {
    const now = new Date();
    const timeDifference = (now - lastPingTime) / 1000; // Convert to seconds

    if (timeDifference > 15) {
        console.log("Ping timeout! Taking action...");
         showToast(TOASTIFY_DISCONNECTED, -1, "warn");
        // Add your reconnection or error-handling logic here
        checkInternetAndReconnect();
    }
}

// Check for ping timeout every 10 seconds
setInterval(checkPingTimeout, 10000);

function getNewSseId(callback) {
    $.get("/api/sse-id", function(response) {
        console.log("New SSE ID received:", response);
        if (callback) callback(response);
    }).fail(function(xhr, status, error) {
        console.error("Failed to get new SSE ID:", error);
    });
}

function fetchSuggestions() {
    if (suggestionsStep<5){
        $.get('/api/apartments/suggestions/' + suggestionsStep, function(response) {
            console.log("fetching suggestions ");
            if (Array.isArray(response)) {
                response.forEach(function(item, index) {
                    $('#suggestions').append(`<button class="suggestion-btn bg-sky-200 rounded-lg p-2 mr-2 hover:bg-sky-300">${item}</button>`);
                });
                suggestionsStep++;
            } else {
                console.error("Response is not an array:", response);
            }
        });
    }
}

function askForResponse() {
    let userMessage = $('#response-container div:last').text();
    $('#message').val('');

    chatRequestTimeout = setTimeout(() => {
        showToast(TOASTIFY_REQUEST_TAKING_LONGER, -1, "warn");
    }, 15000);

//    if (suggestionsStep < 5){
//        userMessage =  lastHelperMessage.trim() + " " +userMessage;
//    }

    htmx.ajax('POST', '/respond', {
        target: '#response-container',
        swap: 'beforeend',
        values: {message: userMessage}
    });

         // Listen for the HTMX response and clear timeout if successful
    $(document).on('htmx:afterRequest', function(event) {
        clearChatRequestTimeout();
        addReportInaccurateResponse();
    });

}

function clearChatRequestTimeout(){
    clearTimeout(chatRequestTimeout); // Clear the timeout
    dismissToast();
}

function applyFavouriteButtonStylingDependingOnText(favouriteButton){
    if (favouriteButton.text() === saveFavouritesTranslated) {
        favouriteButton.addClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700')
                        .removeClass('text-red-500 text-2xl');
    } else if (favouriteButton.html() === '♥')  {
        favouriteButton.addClass('text-red-500 text-2xl').removeClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700');
    }
}

function setCurrentStep(){
    lastAssistantMessage = $('.assistantResponse:last').text();
    if (lastAssistantMessage.trim() === M01_INITIAL_MESSAGE){
        lastHelperMessage = M01_INITIAL_MESSAGE;
        suggestionsStep = 1;
        $('#suggestions').empty();
        fetchSuggestions();
        $('#suggestions').show();
    }
    if (lastAssistantMessage.trim() === M02_CITY)
    {
        lastHelperMessage = M02_CITY;
        suggestionsStep = 2;
        $('#suggestions').empty();
        fetchSuggestions();
        $('#suggestions').show();
    }
    if (lastAssistantMessage.trim() === M03_BUDGET)
    {
        lastHelperMessage = M03_BUDGET;
        suggestionsStep = 3;
        $('#suggestions').empty();
        fetchSuggestions();
        $('#suggestions').show();
    }
    if (lastAssistantMessage.includes(M04_DETAILS_PART_2))
    {
        lastHelperMessage = "";
        suggestionsStep = 4;
        $('#suggestions').empty();
        fetchSuggestions();
        $('#suggestions').show();
    }
}

const reportResponseButton = `
    <button onclick="reportInaccurateResponse()" class="absolute top-[0px] right-[-15px] group" id="reportResponse" title="Report problem">
        <img class="group-hover:hidden" src="/images/pin-error.svg"/>
        <img class="hidden group-hover:block" src="/images/pin-error-hover.svg"/>
    </button>
`;

function addReportInaccurateResponse(){
    $('#reportResponse').remove();
    $('.assistantResponse:last > div:last').append(reportResponseButton);
}

function reportInaccurateResponse(){

    var $button = $('#reportResponse');
    // Disable the button
    $button.prop('disabled', true);

    // Change the image source to the hover image
    $button.find('img').eq(0).hide(); // Hide the first image
    $button.find('img').eq(1).removeClass('hidden group-hover:block').show(); // Remove 'hidden' and 'group-hover:block', then show the second image

    const data =  $('.userMessage:last').text();

    $.ajax({
        url: "/api/chat/report",
        type: "POST",
        contentType: "text/plain",
        data: data,
        success: function (response) {
            showToast("Thank you for reporting an issue", 2000, "info");
        },
        error: function (xhr) {
             showToast("Error occurred while reporting the problem", 3000, "error");
        }
    })

}

function logCallHierarchy() {
  const stackTrace = new Error().stack;
  const lines = stackTrace.split('\n');

  console.log("Call Hierarchy:");
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
      console.log(line);
  }
}