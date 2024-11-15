emailjs.init('Nc9IU4enMFiqEX8QI');

$(document).ready(function(){
    const Joi = window.joi;
    console.log(typeof Joi);

    const schema = Joi.object({
        user_name: Joi.string().required().label('Your Name'),
        user_email: Joi.string().email({ tlds: { allow: false } }).required().label('Your Email'),
        message: Joi.string().required().label('Message')
    });

    $('#contactForm').on('submit', function(event) {
         event.preventDefault(); // Prevent form from refreshing the page

        // Extract form data
        const formData = {
            user_name: $('#user_name').val(),
            user_email: $('#user_email').val(),
            message: $('#message').val(),
        };

        const { error } = schema.validate(formData, { abortEarly: true });
        if (error) {
            // Display validation errors
            const errorMessages = error.details.map(err => err.message).join("\n");
            alert("Validation Errors:\n" + errorMessages);
            return;
        }

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

