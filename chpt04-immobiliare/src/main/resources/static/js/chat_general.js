
$(document).ready(function(){
    // after voice message is transcribed, or text message is added to chat, send a request to obtain a response
    $(document).bind('htmx:load', function(evt) {
        if ((evt.target.parentNode.id === 'response-container')
            && (evt.target.children.length>0)
            && (evt.target.children[0].className.includes("userMessage"))) {
                askForResponse();
        }
    });

    const eventSource = new EventSource("/api/apartments/stream");

    eventSource.addEventListener('apartment', function(event) {
         // Dynamically insert the fragment into the response container
        console.log("Received update:", event.data);
        const container = $('.responseFragmentWithApartments').last()
        const newFragment = event.data;
        console.log(newFragment);
        $('#spinner').hide();
        container.append(newFragment);
    });

    eventSource.addEventListener('response', function(event) {
        // Dynamically insert the fragment into the response container
        const container = $("#response-container").last()
        const newFragment = event.data;
        console.log(newFragment);
        container.append(newFragment);
    });


});

function askForResponse() {
    let userMessage = $('#response-container div:last').text();
    document.getElementById('message').value = '';
    htmx.ajax('POST', '/respond', {target: '#response-container', swap: 'beforeend', values: {message: userMessage}});
}