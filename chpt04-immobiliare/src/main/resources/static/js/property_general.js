$(document).ready(function () {
    // initial page setup
    $('.favouriteButton').each(function () {
        $(this).removeClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700')
                .addClass('text-red-500 text-2xl');
    });


    // on click setup
    $('#response-container').on('click', '.favouriteButton', function () {
        applyFavouriteStylingFor($(this));
    });
});

function applyFavouriteStylingFor(favouriteButton){
    if (favouriteButton.text() === 'Save to Favourites') {
        favouriteButton.html('&hearts;').removeClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700')
                                        .addClass('text-red-500 text-2xl');
    } else if (favouriteButton.html() === '♥')  {
        favouriteButton.text('Save to Favourites').removeClass('text-red-500 text-2xl')
                                                  .addClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700');
    }
}