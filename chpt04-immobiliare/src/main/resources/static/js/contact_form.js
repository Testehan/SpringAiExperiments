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

        const { error } = schema.validate(formData, { abortEarly: false });
        if (error) {
            // Display validation errors
            const errorMessages = error.details.forEach(err => {
                Toastify({
                    text: err.message,
                    duration: 5000,
                    style: {
                        background: "linear-gradient(to right, #fd0713, #ff7675)",
                        color: "white"
                      }
                }).showToast();
            })
            return;
        }

        // Send form data using EmailJS
        emailjs.sendForm('service_07fsa7e', 'template_j12c142', this)
            .then(function(response) {
                Toastify({
                    text: "Message sent successfully!",
                    duration: 3000,
                    style: {
                        background: "linear-gradient(to right, #007bff, #3a86ff)",
                        color: "white"
                      }
                }).showToast();

            }, function(error) {
                Toastify({
                    text: "Failed to send message. Please try again later.",
                    duration: 5000,
                    style: {
                        background: "linear-gradient(to right, #fd0713, #ff7675)",
                        color: "white"
                      }
                }).showToast();
            });

        // Optionally, reset the form after submission
        this.reset();
    });
});

