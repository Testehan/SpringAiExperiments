document.addEventListener('htmx:afterRequest', (event) => {
    if (event.detail.xhr.responseText === 'success') {
        closeLoginModal();
        window.location.reload();
    }
});

function toggleMenu() {
     var menuItems = document.getElementById("menuItems");
     menuItems.classList.toggle("hidden");
}

function checkLogin(event) {
    fetch(event.target.href, { method: "GET" })
        .then(response => {
            if (response.status === 401) { // Unauthorized access, load the modal
                // Prevent the default action of the link
                event.preventDefault();
                openLoginModal();
            }
        });
}

function openLoginModal(){
    document.getElementById("modalLoginContainer").innerHTML = "";
    htmx.ajax("GET", "/login-modal", { target: "#modalLoginContainer", swap: "innerHTML" });
    document.getElementById('modalLoginContainer').classList.remove('hidden');
}

function closeLoginModal() {
    document.getElementById('modalLoginContainer').classList.add('hidden');
    document.getElementById('loginError').innerHTML = '';
}


