$(document).ready(function () {

    // TODO when you release in production, use a new key, that has some domain restriction, meaning
    // that it will only allow requests from your website/domain... need to research this more..
    const apiKey = '';

    // Open the modal
    $('.openMapButton').click(function () {
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


    // TODO this functionality is used in the chat and favourites pages. You should move it to a different js file
    $('.favouriteButton').on('click', function () {
        if ($(this).text() === 'Save to Favourites') {
            $(this).html('&hearts;').removeClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700').addClass('text-red-500 font-bold');
        } else if ($(this).html() === '♥')  {
            $(this).text('Save to Favourites').removeClass('text-red-500 font-bold').addClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700');
        }
    });
});