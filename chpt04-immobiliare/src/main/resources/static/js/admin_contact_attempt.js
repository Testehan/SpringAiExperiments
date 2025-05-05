$(document).ready(function(){

    $("#contactAttemptForm").submit(function(event) {
        handleSubmit(event);
    });

})

async function handleSubmit(event){
    event.preventDefault();

    var valid = true; //await isFormValid();

    if (valid){
        // Collect the form data into an object
        var formData = {
            id : $("#contactAttemptId").val(),
            phoneNumber: $("#phoneNumber").val(),
            listingUrl: $("#listingUrl").val(),
            status: $("#status").val()
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