$(document).ready(function () {
    $(".faq-question, .faq-answer").click(function () {
        // Get the corresponding answer
        const $answer = $(this).hasClass("faq-question")
          ? $(this).next(".faq-answer")
          : $(this);

        // Close all other answers except the clicked one
        $(".faq-answer").not($answer).slideUp();
        $(".chevron").not($(this).closest(".faq-question").find(".chevron")).removeClass("rotate-180");

        // Toggle the clicked answer and chevron
        $answer.slideToggle();
        $(this).closest(".faq-question").find(".chevron").toggleClass("rotate-180");
      });

     // Check if the URL has a hash (e.g., #faq2)
    const hash = window.location.hash;
    if (hash) {
        // Find the FAQ item with the corresponding ID
        const faqItem = $(hash);
        if (faqItem.length) {
            // Find the answer within that FAQ item and remove the 'hidden' class to show it
            faqItem.find('.faq-answer').removeClass('hidden');
        }
    }
});