// Array to store user inputs. Keep in mind that this stores only text messages. If in the future we want to extend
// the functionality to include voice messages, then we would need to add messages to messageInputHistory when they
// are added in the response container (extract message from let userMessage = $('#response-container div:last').text();
// because that contains both text and voice messages)
let messageInputHistory = [];
let historyIndex = -1;


function storeInputInHistory(input) {
    if (input.trim() !== "") {
        messageInputHistory.push(input);
        historyIndex = messageInputHistory.length; // Reset index to one beyond the latest input
    }
}

$(document).ready(function () {
    var messageInput = $("#message");
    $('#sendMessageButton').click(function() {
        $('#spinner').show();
        enableScrollTracking();
        storeInputInHistory(messageInput[0].value);
    });

    // Event listener for detecting key presses
    messageInput.on("keydown", function(event) {

        if (event.key === 'ArrowUp') {
            // Navigate to the previous input
            if (historyIndex > 0) {
                console.log("Arrow up ");
                historyIndex--;
                messageInput[0].value = messageInputHistory[historyIndex];
            }
        } else if (event.key === 'ArrowDown') {
             console.log("Arrow down ");
            // Navigate to the next input
            if (historyIndex < messageInputHistory.length - 1) {
                historyIndex++;
                messageInput[0].value = messageInputHistory[historyIndex];
            } else {
                // If we are at the latest input, clear the input field
                historyIndex = messageInputHistory.length;
                messageInput[0].value = '';
            }
        }
    });
});
