$(document).ready(function(){

    $("#leadForm").submit(function(event) {
        handleSubmit(event);
    });

    $('#downloadBtn').on('click', function () {
        const rawFilterValue = $('#filterInput').val();

        var filterValue = (rawFilterValue && rawFilterValue.trim()) ? rawFilterValue.trim() : null;
        if (filterValue !== null) {
            // Create a hidden link to trigger the CSV download
            const downloadUrl = `/a/leads/download?value=${encodeURIComponent(filterValue)}`;
            const link = $('<a>')
              .attr('href', downloadUrl)
              .attr('download', 'data.csv') // optional: sets a suggested filename
              .appendTo('body');

            link[0].click();
            link.remove();
        } else {
             showToast("You must filter by URL to download", 3000, "error");
        }
    });

})

async function handleSubmit(event){
    event.preventDefault();

    var valid = true; //await isFormValid();

    if (valid){
        var rawCreatedAt = $("#createdAt").val();
        var createdAtTimestamp = null; // Default to null if empty or invalid

        if (rawCreatedAt) { // Check if the string is not empty
            var parsedValue = parseInt(rawCreatedAt, 10);
            if (!isNaN(parsedValue)) { // Check if parsing resulted in a valid number
                createdAtTimestamp = parsedValue;
            } else {
                console.warn("Invalid createdAt value received:", rawCreatedAt);
                // Optional: Handle invalid non-empty input differently if needed
                // You might want to prevent form submission here
            }
        }

        var rawLeadId = $("#leadId").val();
        var LeadId = (rawLeadId && rawLeadId.trim()) ? rawLeadId.trim() : null;

        // Collect the form data into an object
        var formData = {
            id : LeadId,
            phoneNumber: $("#phoneNumber").val(),
            listingUrl: $("#listingUrl").val(),
            status: $("#status").val(),
            createdAt: createdAtTimestamp
        };

        $.ajax({
            url: "/a/leads",
            type: "POST",
            data: JSON.stringify(formData),
            contentType: "application/json",
            success: function (response) {
                showToast(response, 3000);

                $("#leadForm")[0].reset();
                // Delay a little so the toast can be seen before the reload
                setTimeout(function () {
                    location.reload();
                }, 1000); // 1 second delay
            },
            error: function (xhr) {
                console.log(xhr);
                showToast(xhr.responseText, 5000, "error");
            }
        })
    }

}

function populateForm(row) {
    const phone = row.getAttribute('data-phone');
    const url = row.getAttribute('data-url');
    const status = row.getAttribute('data-status');
    const id = row.getAttribute('data-id');
    const createdAt = row.getAttribute('data-createdAt');

    $('#phoneNumber').val(phone);
    $('#listingUrl').val(url);
    $('#status').val(status);
    $('#leadId').val(id);
    $('#createdAt').val(createdAt);

    const formElement = $('#leadForm').get(0);
    if (formElement) { // Check if the element exists before scrolling
        formElement.scrollIntoView({ behavior: 'smooth' });
    }
}

function filterTable() {
    const input = document.getElementById("filterInput").value.toLowerCase();
    const table = document.getElementById("leadsTable");
    const rows = Array.from(table.rows).slice(1);
    rows.forEach(row => {
        const text = row.innerText.toLowerCase();
        row.style.display = text.includes(input) ? "" : "none";
    });
}

function showToast(message, durationMillis, type = "info") {
    if ($(".toastify").length === 0) {
        let bgColor;
        if (type === "error") {
            bgColor = "linear-gradient(to right, #fd0713, #ff7675)";
        } else if (type === "info") {
            bgColor = "linear-gradient(to right, #7ea82b, #c6e28c)";
        } else {
            bgColor = "linear-gradient(to right, #e67e22, #f39c12)"; // Default to "warning"
        }

        Toastify({
            text: message,
            duration: durationMillis, // -1 value Keep it until dismissed
            backgroundColor: bgColor,
            gravity: "top", // Position it at the top
            position: "center", // Center it horizontally
            close: true
        }).showToast();
    }
}