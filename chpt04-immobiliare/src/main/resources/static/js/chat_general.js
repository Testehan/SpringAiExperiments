
$(document).ready(function(){
    // after voice message is transcribed, or text message is added to chat, send a request to obtain a response
    $(document).bind('htmx:load', function(evt) {
        if ((evt.target.parentNode.id === 'response-container') && (evt.target.children[0].className.includes("userMessage"))) {
            askForResponse();
        }
    });
});

function askForResponse() {
    let userMessage = $('#response-container div:last').text();
    document.getElementById('message').value = '';
    htmx.ajax('POST', '/respond', {target: '#response-container', swap: 'beforeend', values: {message: userMessage}});
}