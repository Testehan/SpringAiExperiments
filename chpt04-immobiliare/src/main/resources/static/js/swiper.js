let swiperInstances = []; // Store all swiper instances

function initializeSwiperObjectOnSmallScreens(swiperContainer) {
    if ($(window).width() < 640) { // Only initialize Swiper on small screens

        console.log("Initializing Swiper...");
        const mySwiper = new Swiper(swiperContainer.get(0), {
            direction: 'horizontal',
            autoHeight: true,
//           loop: true,
            pagination: {
                el: ".swiper-pagination",
                clickable: true
            }
        });

        swiperInstances.push(mySwiper);
        updateSwiper(mySwiper);
    }

}

function updateSwiper(swiper) {
    console.log("Update Swiper...");
    swiper.update(); // Updates the Swiper instance with the new slides
    swiper.pagination.update(); // Updates pagination
    swiper.pagination.render(); // Re-renders the pagination bullets
    swiper.pagination.update(); // Ensure pagination is correctly updated
}

// Function to initialize Swipers when new elements are added dynamically
function initializeSwipers() {
    // Initialize Swipers for any .swiper containers on the page
    $('.swiper').each(function () {
        const swiperContainer = $(this);
        if (!swiperContainer.data('swiper-initialized')) { // Check if Swiper has already been initialized
            console.log('Initializing new Swiper for container:', swiperContainer);
            initializeSwiperObjectOnSmallScreens(swiperContainer); // Initialize the Swiper instance
            swiperContainer.data('swiper-initialized', true); // Mark this swiper as initialized
        }
    });
}

$(document).ready(function () {
    initializeSwipers();
});