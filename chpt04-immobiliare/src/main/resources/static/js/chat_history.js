// Array to store user inputs
let inputHistory = [];
let historyIndex = -1; // Index to keep track of the current position in history

// Function to store the input in history
function storeInput(input) {
    if (input.trim() !== "") { // Avoid storing empty inputs
        inputHistory.push(input);
        historyIndex = inputHistory.length; // Reset index to one beyond the latest input
    }
}

$(document).ready(function () {
    var chatInput = $("#message");

    // Event listener for detecting key presses
    chatInput.on("keydown", function(event) {
        console.log("A key was pressed");

        if (event.key === 'ArrowUp') {
        console.log("ArrowUp");
            // Navigate to the previous input
            if (historyIndex > 0) {
                historyIndex--;
                chatInput[0].value = inputHistory[historyIndex];
            }
        } else if (event.key === 'ArrowDown') {
            console.log("ArrowDown");

            // Navigate to the next input
            if (historyIndex < inputHistory.length - 1) {
                historyIndex++;
                chatInput[0].value = inputHistory[historyIndex];
            } else {
                // If we are at the latest input, clear the input field
                historyIndex = inputHistory.length;
                chatInput[0].value = '';
            }
        } else if (event.key === 'Enter') {
             console.log("Enter");

            // Store the input when Enter is pressed
            storeInput(chatInput[0].value);
            $("#sendMessageButton").click();

//            chatInput[0].value = ''; // Clear the input field after sending the message
        }
    });
});