let suggestionsStep = 1;

let eventSource;
let pingTimeout;
let isCheckingInternet = false;

let mediaRecorder;
let audioChunks = [];

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
        connectToSSE(newSseId.sseId);
    });

//    // todo i am trying to see if this fixes the issue on iphone ???
//    window.addEventListener('online', function() {
//        dismissToast();
//        reconnect();
//    });

    setCurrentStep();
    setUpScrollingToLastUserMessage();

    $(document).on("click", ".suggestion-btn", function (e) {
        const suggestionText = $(this).text(); // Get button text
        $("#message").val(suggestionText); // Populate input box
        $('#spinner').show();
        $("#sendMessageButton").click();
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
        } else {
            mediaRecorder.stop();
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
                container.append(newFragment);
            }

        });

        eventSource.addEventListener('keep-alive', function(event) {
            if (event.data === 'ping') {
                clearTimeout(pingTimeout); // Prevent multiple timeouts
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

function getNewSseId(callback) {
    $.get("/api/sse-id", function(response) {
        console.log("New SSE ID received:", response);
        if (callback) callback(response);
    }).fail(function(xhr, status, error) {
        console.error("Failed to get new SSE ID:", error);
    });
}

function fetchSuggestions() {
    if (suggestionsStep<4){
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
    console.log("asking for response");
    let userMessage = $('#response-container div:last').text();
    $('#message').val('');
    htmx.ajax('POST', '/respond', {target: '#response-container', swap: 'beforeend', values: {message: userMessage}});
}

function setUpScrollingToLastUserMessage(){
    const targetClassName = "userMessage";

    const observer = new MutationObserver((mutationsList) => {
        mutationsList.forEach((mutation) => {
            if (mutation.type === "childList") {

                const targetElements = document.querySelectorAll(`.${targetClassName}`);
                const lastElement = targetElements[targetElements.length - 1];
                const container = $('#response-container');

                // hide spinner when we get an assistant response, that is not obtained via SSE
                const addedNodes = mutation.addedNodes;
                for (const node of addedNodes) {
                    if (node.nodeType === Node.ELEMENT_NODE && node.childNodes[1].classList.contains('assistantResponse')) {
                        $('#spinner').hide();
                        setCurrentStep();
                        if (node.childNodes[1].innerText === M04_APARTMENTS_FOUND_START){
                            $('#suggestions').hide(); // at this point we don't care about suggestions anymore because the user enters his description
                        }
                    }

// TODO if no more issues with autoscrolling this can be removed
//                    if (lastElement && node.nodeType === Node.ELEMENT_NODE && node.parentNode ===  $("#response-container")[0]) {
//                        lastElement.scrollIntoView({ behavior: "smooth" });
//                    }

                    if (node.nodeType === Node.ELEMENT_NODE && ( node.childNodes[1].classList.contains('assistantResponse') ||
                        node.childNodes[1].classList.contains('userMessage')))
                    {
                        if (lastElement && container.has(lastElement).length) {
                            const lastElementRect = $(lastElement)[0].getBoundingClientRect();
                            const containerRect = container[0].getBoundingClientRect();

                            // Calculate the new scroll position to bring the target element to the top of the container
                            const scrollOffset = container.scrollTop() + (lastElementRect.top - containerRect.top);
                            // Scroll the container to the calculated position
                            container.animate({ scrollTop: scrollOffset }, 'smooth');
                        }
                    }

                }
            }
        });
    });

    // Observe the container where dynamic content is added
    const container = $("#response-container")[0];
    if (container) {
        observer.observe(container, { childList: true, subtree: true });
    } else {
        console.error("Container not found");
    }

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
    const lastAssistantMessage = $('.assistantResponse:last').text();
    if (lastAssistantMessage.trim() === M01_INITIAL_MESSAGE){
        suggestionsStep = 1;
        $('#suggestions').empty();
        fetchSuggestions();
        $('#suggestions').show();
    }
    if (lastAssistantMessage.trim() === M02_CITY)
    {
        suggestionsStep = 2;
        $('#suggestions').empty();
        fetchSuggestions();
        $('#suggestions').show();
    }
    if (lastAssistantMessage.includes(M03_DETAILS_PART_2))
    {
        suggestionsStep = 3;
        $('#suggestions').empty();
        fetchSuggestions();
        $('#suggestions').show();
    }
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