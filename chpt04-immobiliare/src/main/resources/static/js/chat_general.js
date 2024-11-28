
$(document).ready(function(){
    // after voice message is transcribed, or text message is added to chat, send a request to obtain a response
    $(document).bind('htmx:load', function(evt) {
        if ((evt.target.parentNode.id === 'response-container')
            && (evt.target.children.length>0)
            && (evt.target.children[0].className.includes("userMessage"))) {
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
            htmx.process($('#response-container')[0]);
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

   setUpScrollingToLastUserMessage();

});

function askForResponse() {
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
                    }

                    if (lastElement && node.nodeType === Node.ELEMENT_NODE && node.parentNode ===  $("#response-container")) {
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