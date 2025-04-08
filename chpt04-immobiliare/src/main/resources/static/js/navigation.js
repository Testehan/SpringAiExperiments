document.addEventListener('htmx:afterRequest', (event) => {
    if (event.detail.xhr.responseText === 'success') {
        closeLoginModal();
        window.location.reload();
    }
});

$(document).ready(function() {
    $("#menuButton").click(function() {
        toggleMenu();
    });

    // Close the menu when clicking anywhere else
    $(document).click(function (event) {
        // Check if the menu is visible and the click is outside the menu and button
        if (!$(event.target).closest("#menuItems, #menuButton").length) {
            $("#menuItems").addClass("hidden"); // Hide the menu
        }
    });

});

function toggleMenu() {
    $("#menuItems").toggleClass("hidden");
}

function checkLogin(event) {
    fetch(event.target.href, { method: "GET" })
        .then(response => {
            if (response.status === 401) { // Unauthorized access, load the modal
                // Prevent the default action of the link
                event.preventDefault();
                openLoginModal();
            } else {
                // User is authenticated, navigate to the requested page
                window.location.href = event.target.href;
            }
        });
}

function openLoginModal(){
    $("#modalLoginContainer").html("");
    htmx.ajax("GET", "/login-modal", { target: "#modalLoginContainer", swap: "innerHTML" });
    $("#modalLoginContainer").removeClass('hidden');
}

function closeLoginModal() {
    $("#modalLoginContainer").addClass('hidden');
    $("#loginError").html('');
    //setTimeout(function() {     // redirect to homepage if user decides not to login
    //    window.location.href = "/";
    //}, 0); // 0 milliseconds = 0 seconds
}


