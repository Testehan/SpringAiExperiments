$(document).ready(function () {
    // initial page setup
    $('.favouriteButton').each(function () {
        applyInitialFavouriteStylingFor($(this));
    });


    // on click setup
    $('#response-container').on('click', '.favouriteButton', function () {
        applyFavouriteStylingFor($(this));
    });

    $('button[data-apartment-id]').on('click', function() {
        getPhoneNumber($(this));
    });

});

function getPhoneNumber(showContactButton){
    var $button = showContactButton; // The button that was clicked
    var apartmentId = $button.data('apartment-id'); // Get the apartment ID

    // Make the AJAX request to get the phone number
    $.ajax({
        url: APP_URL + '/api/apartments/contact/' + apartmentId, // Make sure this is the correct endpoint
        method: 'GET', // Assuming a GET request
        success: function(response) {
            var phoneNumber = response;

            var $parentSpan = $button.closest('span');
            // Remove the button
            $button.remove();

            var $phoneNumberElement = $('<span>').text(phoneNumber).css('line-height', '32px');
            $parentSpan.append($phoneNumberElement);

            if (response !== 'No apartment found!'){
                // Construct the WhatsApp link
                var whatsappLink = 'https://wa.me/' + '4' + phoneNumber;

                // Find the corresponding WhatsApp link element and update it
                var $whatsappLink = $parentSpan.closest('span').find('.whatsapp-link');
                $whatsappLink.attr('href', whatsappLink).show(); // Set the href and show the link

                $parentSpan.append($whatsappLink);
            }

        },
        error: function() {
            console.error('Failed to get phone number');
        }
    });
}


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