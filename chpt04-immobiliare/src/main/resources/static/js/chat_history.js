// Array to store user inputs
let messageInputHistory = [];
let historyIndex = -1; // Index to keep track of the current position in history

// Function to store the input in history
function storeInput(input) {
    if (input.trim() !== "") {
        messageInputHistory.push(input);
        historyIndex = messageInputHistory.length; // Reset index to one beyond the latest input
    }
}

$(document).ready(function () {
    var messageInput = $("#message");

    // Event listener for detecting key presses
    messageInput.on("keydown", function(event) {

        if (event.key === 'ArrowUp') {
            // Navigate to the previous input
            if (historyIndex > 0) {
                historyIndex--;
                messageInput[0].value = messageInputHistory[historyIndex];
            }
        } else if (event.key === 'ArrowDown') {

            // Navigate to the next input
            if (historyIndex < messageInputHistory.length - 1) {
                historyIndex++;
                messageInput[0].value = messageInputHistory[historyIndex];
            } else {
                // If we are at the latest input, clear the input field
                historyIndex = messageInputHistory.length;
                messageInput[0].value = '';
            }
        } else if (event.key === 'Enter') {
            storeInput(messageInput[0].value);
//            $("#sendMessageButton").click();
        }
    });
});