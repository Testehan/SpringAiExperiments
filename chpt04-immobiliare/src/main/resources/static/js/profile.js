var fullInviteUrl;

$(document).ready(function() {
    var input = $('#inviteOthers');
    fullInviteUrl = input.val();
    var truncatedText = fullInviteUrl.substring(0, 30) + "...";
    input.val(truncatedText);

});

function copyToClipboard() {

    navigator.clipboard.writeText(fullInviteUrl).then(function() {
        Toastify({
            text: "Text copied to clipboard!",
            duration: 2000,
            style: {
                background: "linear-gradient(to right, #007bff, #3a86ff)",
                color: "white"
              }
        }).showToast();
    }).catch(function(err) {
        console.error("Failed to copy text: ", err);
        Toastify({
            text: "Failed to copy text",
            duration: 5000,
            style: {
                background: "linear-gradient(to right, #fd0713, #ff7675)",
                color: "white"
              }
        }).showToast();
    });
}

function shareOnSocialMedia() {
    var titleWithPrice = 'Invite CasaMia.ai'

    if (navigator.share) {
      navigator.share({
        title: titleWithPrice,
        url: fullInviteUrl
      })
      .then(() => console.log('Successful share'))
      .catch((error) => console.error('Error sharing:', error));
    } else {
      // Fallback for browsers that don't support the Web Share API
      alert(SHARE_ERROR);
    }
}