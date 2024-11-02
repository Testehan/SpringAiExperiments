
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
    document.getElementById('message').value = '';
    htmx.ajax('POST', '/respond', {target: '#response-container', swap: 'beforeend', values: {message: userMessage}});
}

function setUpScrollingToLastUserMessage(){
    const targetClassName = "userMessage";

    const observer = new MutationObserver((mutationsList) => {
        mutationsList.forEach((mutation) => {
            if (mutation.type === "childList") {
//                console.log("Detected mutation:", mutation);

                const targetElements = document.querySelectorAll(`.${targetClassName}`);
                const lastElement = targetElements[targetElements.length - 1];
                if (lastElement) {
                    lastElement.scrollIntoView({ behavior: "smooth" });
                }
            }
        });
    });

    // Observe the container where dynamic content is added
    const container = document.getElementById("response-container");
    if (container) {
        observer.observe(container, { childList: true, subtree: true });
    } else {
        console.error("Container not found");
    }

}