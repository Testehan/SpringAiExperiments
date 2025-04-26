let isUserScrolling = false;
let hasUserScrolledRecently = false;
let isAutoScrolling = false;                // flag that tells if scrolling is done automatically

let scrollTimeout;
// Define in one JS file (e.g., globals.js)
window.isScrollTrackingEnabled = true;

$(document).ready(function(){

    setUpScrollingToLastUserMessage();

   // Attach event listener
   $(window).on('scroll', handleScroll);
   $('#response-container').on('scroll', handleScroll);

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
                        if (node.childNodes[1].innerText.includes(M05_APARTMENTS_FOUND_START)){
                            $('#suggestions').hide(); // at this point we don't care about suggestions anymore because the user enters his description

                        }
                        if (isUserScrolling){
                            hasUserScrolledRecently = true;
                            disableScrollTracking();
                        }

//                        if (node.childNodes[1].innerText === decodeHtmlEntities(M05_APARTMENTS_FOUND_END)){
//                            console.log("Enabling scroll tracking");
//                            enableScrollTracking();
//                        }
                    }

                    if (node.nodeType === Node.ELEMENT_NODE && ( node.childNodes[1].classList.contains('assistantResponse') ||
                        node.childNodes[1].classList.contains('userMessage')))
                    {
                        if (!hasUserScrolledRecently && lastElement && container.has(lastElement).length) {
                            const lastElementRect = $(lastElement)[0].getBoundingClientRect();
                            const containerRect = container[0].getBoundingClientRect();

                            // Calculate the new scroll position to bring the target element to the top of the container
                            const scrollOffset = container.scrollTop() + (lastElementRect.top - containerRect.top);

                             isAutoScrolling = true;
                            // Scroll the container to the calculated position
                            container.animate({ scrollTop: scrollOffset }, 'smooth', () => {
                                // **After auto-scroll completes, reset flag**
                                isAutoScrolling = false;
                            });
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

function handleScroll() {
    if (!window.isScrollTrackingEnabled || isAutoScrolling) return; // Disable effect if condition is met

    isUserScrolling = true;

    clearTimeout(scrollTimeout);
    scrollTimeout = setTimeout(() => {
        isUserScrolling = false;
    }, 5000); // Reset after 5 seconds of no scrolling
}

// Function to disable scrolling behavior
function disableScrollTracking() {
    window.isScrollTrackingEnabled = false;

    $(window).off('scroll', handleScroll); // Detach event listener
    $('#response-container').off('scroll', handleScroll);
    window.isScrollListenerAttached = false
}

// Function to enable scrolling behavior
function enableScrollTracking() {
     console.log("Re-enabling scroll tracking...");
     window.isScrollTrackingEnabled = true;
     hasUserScrolledRecently = false;

     // Ensure the scroll event is reattached if necessary
     if (!window.isScrollListenerAttached) {
         $(window).on('scroll', handleScroll);
          $('#response-container').on('scroll', handleScroll);
         window.isScrollListenerAttached = true;
     }
}

function decodeHtmlEntities(encodedString) {
    const parser = new DOMParser();
    const decodedString = parser.parseFromString(encodedString, "text/html").body.textContent;
    return decodedString;
}