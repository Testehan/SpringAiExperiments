
$(document).ready(function(){

    $('#deleteButton').on('click', function (event) {
        event.preventDefault(); // Prevent default form submission
        $('#deletion_confirmation').removeClass('hidden'); // Show the modal
        const deleteUrl = $(this).data('url'); // Get URL from button data-url
        $('#confirmDelete').data('url', deleteUrl); // Store URL in confirm button
    });

    // When the user clicks the "Yes, Delete" button
    $('#confirmDelete').on('click', function() {
        // Get the URL from the data attribute
        const url = $(this).data('url');

        // Send the POST request
        $.ajax({
          url: url,
          type: 'POST',
          success: function (response) {
            // Handle success (e.g., show a success message or reload the page)
            $('#deletion_confirmation').addClass('hidden');
            Toastify({
                text: response,
                duration: 4000,
                style: {
                  background: "linear-gradient(to right, #007bff, #3a86ff)",
                  color: "white"
                }
            }).showToast();

            setTimeout(function() {
                window.location.href = "add";
              }, 3000); // 3000 milliseconds = 3 seconds
          },
          error: function (xhr, status, error) {
            // Handle error (e.g., show an error message)
            $('#deletion_confirmation').addClass('hidden');
            Toastify({
               text: xhr.responseText,
               duration: 3000,
               gravity: "top",
               position: "right",
               style: {
                 background: "linear-gradient(to right, #fd0713, #ff7675)",
                 color: "white"
               }
            }).showToast();
          }
        });
    });

    // When the user clicks the "Cancel" button
    $('#cancelDelete').on('click', function() {
        $('#deletion_confirmation').addClass('hidden'); // Hide the modal
    });

});
