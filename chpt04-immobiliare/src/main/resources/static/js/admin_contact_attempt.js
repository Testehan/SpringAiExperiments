$(document).ready(function(){

    $("#contactAttemptForm").submit(function(event) {
        handleSubmit(event);
    });

})

async function handleSubmit(event){
    event.preventDefault();

    var valid = true; //await isFormValid();

    if (valid){
        var rawCreatedAt = $("#createdAt").val();
        var createdAtTimestamp = null; // Default to null if empty or invalid

        if (rawCreatedAt) { // Check if the string is not empty
            var parsedValue = parseInt(rawCreatedAt, 10);
            if (!isNaN(parsedValue)) { // Check if parsing resulted in a valid number
                createdAtTimestamp = parsedValue;
            } else {
                console.warn("Invalid createdAt value received:", rawCreatedAt);
                // Optional: Handle invalid non-empty input differently if needed
                // You might want to prevent form submission here
            }
        }

        var rawContactAttemptId = $("#contactAttemptId").val();
        var contactAttemptId = (rawContactAttemptId && rawContactAttemptId.trim()) ? rawContactAttemptId.trim() : null;

        // Collect the form data into an object
        var formData = {
            id : contactAttemptId,
            phoneNumber: $("#phoneNumber").val(),
            listingUrl: $("#listingUrl").val(),
            status: $("#status").val(),
            createdAt: createdAtTimestamp
        };

        $.ajax({
            url: "/a/contact-attempts",
            type: "POST",
            data: JSON.stringify(formData),
            contentType: "application/json",
            success: function (response) {
                Toastify({
                    text: response,
                    duration: 3000,
                    style: {
                      background: "linear-gradient(to right, #007bff, #3a86ff)",
                      color: "white"
                    }
                }).showToast();

                $("#contactAttemptForm")[0].reset();
                // Delay a little so the toast can be seen before the reload
                setTimeout(function () {
                    location.reload();
                }, 1000); // 1 second delay
            },
            error: function (xhr) {
                console.log(xhr);

                Toastify({
                    text: xhr.responseText,
                    duration: 5000,
                    gravity: "top",
                    position: "right",
                    style: {
                      background: "linear-gradient(to right, #fd0713, #ff7675)",
                      color: "white"
                    }
                }).showToast();
            }
        })
    }

}