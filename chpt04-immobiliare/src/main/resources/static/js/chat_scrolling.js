let isUserScrolling = false;
let scrollTimeout;

$(document).ready(function(){

    setUpScrollingToLastUserMessage();

    $(window).on('scroll', function () {
        isUserScrolling = true;

        // Set a timeout to reset `isUserScrolling` after the user stops scrolling
        clearTimeout(scrollTimeout);
        scrollTimeout = setTimeout(() => {
            console.log("isUserScrolling " + isUserScrolling);
            isUserScrolling = false;
        }, 3000); // 1 second delay after scrolling stops
    });


});


function setUpScrollingToLastUserMessage(){
    const targetClassName = "userMessage";

    const observer = new MutationObserver((mutationsList) => {
        mutationsList.forEach((mutation) => {
            if (mutation.type === "childList") {

                const targetElements = document.querySelectorAll(`.${targetClassName}`);
                const lastElement = targetElements[targetElements.length - 1];
                const container = $('#response-container');

                // hide spinner when we get an assistant response, that is not obtained via SSE
                const addedNodes = mutation.addedNodes;
                for (const node of addedNodes) {
                    if (node.nodeType === Node.ELEMENT_NODE && node.childNodes[1].classList.contains('assistantResponse')) {
                        $('#spinner').hide();
                        setCurrentStep();
                        if (node.childNodes[1].innerText === M05_APARTMENTS_FOUND_START){
                            $('#suggestions').hide(); // at this point we don't care about suggestions anymore because the user enters his description
                        }
                    }

                    if (node.nodeType === Node.ELEMENT_NODE && ( node.childNodes[1].classList.contains('assistantResponse') ||
                        node.childNodes[1].classList.contains('userMessage')))
                    {
                        if (!isUserScrolling && lastElement && container.has(lastElement).length) {
                            const lastElementRect = $(lastElement)[0].getBoundingClientRect();
                            const containerRect = container[0].getBoundingClientRect();

                            console.log("this should be true cause it will scroll isUserScrolling " + isUserScrolling);

                            // Calculate the new scroll position to bring the target element to the top of the container
                            const scrollOffset = container.scrollTop() + (lastElementRect.top - containerRect.top);
                            // Scroll the container to the calculated position
                            container.animate({ scrollTop: scrollOffset }, 'smooth');
                        }
                    }

                }
            }
        });
    });

    // Observe the container where dynamic content is added
    const container = $("#response-container")[0];
    if (container) {
        observer.observe(container, { childList: true, subtree: true });
    } else {
        console.error("Container not found");
    }

}