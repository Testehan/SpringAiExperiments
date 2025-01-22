let mySwiper; // Declare globally (but don't initialize yet)

function initializeSwiperObjectOnSmallScreens() {
    if ($(window).width() < 640) { // Only initialize Swiper on small screens
        const swiperContainer = $('.swiper'); // Select .swiper elements
        if (swiperContainer.length === 0) {
            console.warn("No '.swiper' elements found. Skipping initialization.");
            return;
        }

        console.log("Initializing Swiper...");
        mySwiper = new Swiper('.swiper', {
            direction: 'horizontal',
            loop: true,
            autoHeight: true,
            pagination: {
                el: ".swiper-pagination",
                clickable: true
            }
        });
    }

}

$(document).ready(function () {
    initializeSwiperObjectOnSmallScreens();
});