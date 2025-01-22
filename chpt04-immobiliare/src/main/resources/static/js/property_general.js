$(document).ready(function () {
    // initial page setup
    $('.favouriteButton').each(function () {
        applyInitialFavouriteStylingFor($(this));
    });


    // on click setup
    $('#response-container').on('click', '.favouriteButton', function () {
        applyFavouriteStylingFor($(this));
    });

    if ($(window).width() < 640) { // Only initialize Swiper on small screens
        new Swiper('.swiper', {
            direction: 'horizontal',
            loop: true,
            autoHeight: true,
            pagination: {
                el: ".swiper-pagination",
            }
        });
    }

});

function applyFavouriteStylingFor(favouriteButton){
    if (favouriteButton.text() === saveFavouritesTranslated) {
        favouriteButton.html('&hearts;').removeClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700')
                                        .addClass('text-red-500 text-2xl');
    } else if (favouriteButton.html() === '♥')  {
        favouriteButton.text(saveFavouritesTranslated).removeClass('text-red-500 text-2xl')
                                                  .addClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700');
    }
}

function applyInitialFavouriteStylingFor(favouriteButton){
    if (favouriteButton.text() === saveFavouritesTranslated) {
        favouriteButton.addClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700')
                        .removeClass('text-red-500 text-2xl');
    } else if (favouriteButton.html() === '♥')  {
        favouriteButton.addClass('text-red-500 text-2xl')
                      .removeClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700');
    }
}

// Open the lightbox with the clicked image
function openLightbox(src) {
    $('#lightbox-img').attr('src', src);
    $('#lightbox').css('display', 'flex');
}

// Close the lightbox
function closeLightbox() {
    $('#lightbox').css('display', 'none');
}

function shareOnSocialMedia(id, title, price) {
    var urlWithId = APP_URL + '/view/' + id;
    var titleWithPrice = title + ' - ' + price+' euro'

    if (navigator.share) {
      navigator.share({
        title: titleWithPrice,
        url: urlWithId
      })
      .then(() => console.log('Successful share'))
      .catch((error) => console.error('Error sharing:', error));
    } else {
      // Fallback for browsers that don't support the Web Share API
      alert(SHARE_ERROR);
    }
  }