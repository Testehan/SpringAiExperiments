let suggestionsStep = 1;;

$(document).ready(function(){
    // after voice message is transcribed, or text message is added to chat, send a request to obtain a response
    $(document).bind('htmx:load', function(evt) {
        if ((evt.target.parentNode.id === 'response-container')
            && (evt.target.children.length>0)
            && (evt.target.children[0].className.includes("userMessage"))) {
                logCallHierarchy();
                askForResponse();
        }
    });


    console.log("SSE ID:", sseId);
    const eventSourceUrl = "/api/apartments/stream/"+sseId;
    console.log("eventSourceUrl" + eventSourceUrl)
    const eventSource = new EventSource(eventSourceUrl);

    eventSource.addEventListener('apartment', function(event) {
        if (event.lastEventId === sseId) {
             // Dynamically insert the fragment into the response container
            const container = $('.responseFragmentWithApartments').last()
            const newFragment = event.data;
            $('#spinner').hide();
            container.append(newFragment);

            applyFavouriteButtonStylingDependingOnText($('.favouriteButton').last());

            // we need this in order to have the Contact and Favourite working when obtaining html string containing htmx from server
            logCallHierarchy();
            console.log("processed an apartment");
            htmx.process($('#response-container')[0]);
        }
    });

    eventSource.addEventListener('response', function(event) {
         if (event.lastEventId === sseId) {
            logCallHierarchy();
            console.log("processed a response");

            // Dynamically insert the fragment into the response container
            const container = $("#response-container").last()
            const newFragment = event.data;
            $('#spinner').hide();
            container.append(newFragment);
        }

    });

   setCurrentStep();
//   fetchSuggestions();
   setUpScrollingToLastUserMessage();

   $(document).on("click", ".suggestion-btn", function (e) {
         console.log("suggestion butoon clicked");
         logCallHierarchy();
// todo don't think below i is needed
         if (e.key === "Enter") {
             e.preventDefault(); // Ignore Enter key
             return;
         }

        const suggestionText = $(this).text(); // Get button text
        $("#message").val(suggestionText); // Populate input box
        $('#spinner').show();
        $("#sendMessageButton").click();
    });

});

function fetchSuggestions() {
    if (suggestionsStep<4){
        $.get('/api/apartments/suggestions/' + suggestionsStep, function(response) {
            console.log("fetching suggestions ");
            if (Array.isArray(response)) {
                response.forEach(function(item, index) {
                    $('#suggestions').append(`<button class="suggestion-btn inline-block bg-sky-200 rounded-lg p-2 mr-2 hover:bg-sky-300">${item}</button>`);
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

                // hide spinner when we get an assistant response, that is not obtained via SSE
                const addedNodes = mutation.addedNodes;
                for (const node of addedNodes) {
                    if (node.nodeType === Node.ELEMENT_NODE && node.childNodes[1].classList.contains('assistantResponse')) {
                        $('#spinner').hide();
                        setCurrentStep();
                        if (node.childNodes[1].innerText === "Searching for properties based on the provided description."){
                            $('#suggestions').hide(); // at this point we don't care about suggestions anymore because the user enters his description
                        }
                    }

                    if (lastElement && node.nodeType === Node.ELEMENT_NODE && node.parentNode ===  $("#response-container")[0]) {
                        lastElement.scrollIntoView({ behavior: "smooth" });
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
    if (favouriteButton.text() === 'Save to Favourites') {
        favouriteButton.addClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700')
                        .removeClass('text-red-500 text-2xl');
    } else if (favouriteButton.html() === '♥')  {
        favouriteButton.addClass('text-red-500 text-2xl').removeClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700');
    }
}

function setCurrentStep(){
    const lastAssistantMessage = $('.assistantResponse:last').text();
    if (lastAssistantMessage.trim() === "Hi..are you interested in apartments for rent or sale ?"){
        suggestionsStep = 1;
        $('#suggestions').empty();
        fetchSuggestions();
        $('#suggestions').show();
    }
    if (lastAssistantMessage.trim() === "Which city are you interested in ?") {
        $('#suggestions').empty();
        fetchSuggestions();
        $('#suggestions').show();
        suggestionsStep = 2;
    }
    if (lastAssistantMessage.includes("You are looking for properties for") &&
        lastAssistantMessage.includes("Give me more details about the location you are searching for"))
    {
        $('#suggestions').empty();
        fetchSuggestions();
        $('#suggestions').show();
        suggestionsStep = 3;
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