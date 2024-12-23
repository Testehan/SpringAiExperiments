$(document).ready(function () {

    // TODO when you release in production, use a new key, that has some domain restriction, meaning
    // that it will only allow requests from your website/domain... need to research this more..
    const apiKey = 'AIzaSyDmZ_CkQKsJxlsTT8QyVuNsJuV8oIqc0RU';

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