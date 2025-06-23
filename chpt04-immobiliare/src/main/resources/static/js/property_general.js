const WHATSAPP_BASE_URL = 'https://wa.me/';

$(document).ready(function () {
    // initial page setup
    $('.favouriteButton').each(function () {
        applyInitialFavouriteStylingFor($(this));
    });


    // on click setup
    $('#response-container').on('click', '.favouriteButton', function () {
        applyFavouriteStylingFor($(this));
    });

    $('#response-container').on('click', '.contactButton', function() {
        getPhoneNumber($(this));
    });

});

function getPhoneNumber(showContactButton){
    var $button = showContactButton; // The button that was clicked
    var apartmentId = $button.data('apartment-id'); // Get the apartment ID
    var ownerName = $button.data('owner-name');

    fetch(APP_URL + '/api/apartments/contact/' + apartmentId, { method: "GET" })
        .then(response => {
            if (response.redirected) {
                // If redirected, user is not logged in → Open login modal
                openLoginModal();
                throw new Error("Unauthorized - Redirected to login");
            }

            if (response.ok) {
                return response.text();  // .text() returns a Promise, so we return it to resolve later
            }
        })
        .then(phoneNumber => {

            var $parentSpan = $button.closest('span');
            $button.remove();

            var $ownerNameElement = $('<span>').text(ownerName).css('line-height', '32px').addClass('mr-2');
            var $phoneNumberElement = $('<a>')
                .attr('href', 'tel:' + phoneNumber)
                .css('line-height', '32px')
                .text(phoneNumber);
            $parentSpan.append($ownerNameElement);
            $parentSpan.append($phoneNumberElement);

            if (phoneNumber !== 'No apartment found!'){
                var whatsappLink = WHATSAPP_BASE_URL + phoneNumber;
                var $whatsappLink = $parentSpan.closest('span').find('.whatsapp-link');
                $whatsappLink.attr('href', whatsappLink).show();
                $parentSpan.append($whatsappLink);
            }
        })
        .catch(error => {
            if (error.message !== "Unauthorized") {
                console.error('Failed to get phone number:', error);
            }
        });
}


function applyFavouriteStylingFor(favouriteButton){
    var $button = favouriteButton; // The button that was clicked
    var apartmentId = $button.data('apartment-id'); // Get the apartment ID

     fetch(APP_URL + '/api/apartments/favourite/' + apartmentId, { method: "GET" })
            .then(response => {
                if (response.redirected) {
                    // If redirected, user is not logged in → Open login modal
                    openLoginModal();
                    throw new Error("Unauthorized - Redirected to login");
                }

               if (favouriteButton.text() === saveFavouritesTranslated) {
                    favouriteButton.html('&hearts;').removeClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700')
                                                   .addClass('text-red-500 text-2xl');
               } else if (favouriteButton.html() === '♥')  {
                    favouriteButton.text(saveFavouritesTranslated).removeClass('text-red-500 text-2xl')
                                                                 .addClass('bg-blue-500 text-white px-2 rounded w-fit hover:bg-blue-700');
               }

            })
            .catch(error => {
                if (error.message !== "Unauthorized") {
                    console.error('Failed to set favourite:', error);
                }
            });


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