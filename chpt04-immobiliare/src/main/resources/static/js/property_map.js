$(document).ready(function () {

    const apiKey = GOOGLE_MAPS_API_KEY;

    // Open the modal
    $('#response-container').on('click', '.openMapButton', function () {
        const location = $(this).data('location'); // Get location from button
        const mapUrl = `https://www.google.com/maps/embed/v1/place?key=${apiKey}&q=${encodeURIComponent(location)}`;

        // Set the iframe source dynamically
        $('#mapFrame').attr('src', mapUrl);

        // Show the modal
        $('#mapModal').removeClass('hidden');
    });

    // Close the modal
    $('#closeMapModal').click(function () {
        $('#mapModal').addClass('hidden');
        $('#mapFrame').attr('src', ''); // Clear iframe src to stop the map
    });

    // Optional: Close modal by clicking outside of it
    $('#mapModal').click(function (e) {
        if ($(e.target).is('#mapModal')) {
            $(this).addClass('hidden');
            $('#mapFrame').attr('src', ''); // Clear iframe src
        }
    });

});