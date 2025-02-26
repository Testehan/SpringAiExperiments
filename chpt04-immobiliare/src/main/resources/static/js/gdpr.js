$(document).ready(function () {
  const consentKey = 'gdprConsent';
  const consentSavedInDBKey = 'consentSavedInDB';

  // Check if consent is already given
  const consentGiven = localStorage.getItem(consentKey);
  const consentSavedInDB = localStorage.getItem(consentSavedInDBKey);
  if (!consentGiven || consentGiven === 'false') {
    $('#gdpr-consent-banner').removeClass('hidden');
  } else {
    if (consentGiven && consentSavedInDB === 'false'){
        storeConsent(true);
    }
  }

  // Accept button click event
  $('#gdpr-accept-btn').on('click', function () {
    storeConsent(true);
    $('#gdpr-consent-banner').addClass('hidden');
  });

  // Decline button click event
  $('#gdpr-decline-btn').on('click', function () {
    storeConsent(false);
    $('#gdpr-consent-banner').addClass('hidden');
  });

  // Function to store consent and send to server
  function storeConsent(isAccepted) {
    localStorage.setItem(consentKey, isAccepted);
    $.ajax({
      url: '/accept-gdpr',
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify({
        consent: isAccepted,
        timestamp: new Date().toISOString(),
      }),
      success: function (response) {
        console.log('Consent stored:', response);
        localStorage.setItem(consentSavedInDBKey, true);
      },
      error: function (error) {
        console.error('Error storing consent:', error);
         localStorage.setItem(consentSavedInDBKey, false);
      },
    });
  }

  $('#deleteUserForm').submit(function(event) {
    event.preventDefault();

    // Remove specific items from localStorage
    localStorage.removeItem(consentKey);
    localStorage.removeItem(consentSavedInDBKey);

    this.submit();
  });

});
