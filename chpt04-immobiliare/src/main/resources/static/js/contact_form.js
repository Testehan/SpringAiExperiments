emailjs.init('Nc9IU4enMFiqEX8QI');

$(document).ready(function(){
    $('#contactForm').on('submit', function(event) {
         event.preventDefault(); // Prevent form from refreshing the page

        // Send form data using EmailJS
        emailjs.sendForm('service_07fsa7e', 'template_j12c142', this)
            .then(function(response) {
                console.log('SUCCESS!', response.status, response.text);
                alert('Message sent successfully!');
            }, function(error) {
                console.error('FAILED...', error);
                alert('Failed to send message. Please try again later.');
            });

        // Optionally, reset the form after submission
        this.reset();
    });
});

