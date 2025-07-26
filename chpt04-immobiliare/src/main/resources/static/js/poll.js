const pollId = 'poll1';

function loadPoll() {
    $.get(`/api/poll/${pollId}`, function(poll) {
      $('#poll-question').text(poll.question);
      const container = $('#poll-options');
      container.empty();

      $.each(poll.options, function(option) {
        container.append(`
            <label class="flex items-center space-x-2 p-2 rounded-lg hover:bg-gray-100 cursor-pointer">
              <input type="radio" name="vote" value="${option}" class="text-blue-600 focus:ring-blue-500">
              <span class="text-gray-800">${option}</span>
            </label>
        `);
      });
    });
}

function showResults(poll) {
    const resultDiv = $('#poll-result');
    resultDiv.empty().show();

    // Calculate total votes
    const totalVotes = Object.values(poll.options).reduce((a, b) => a + b, 0) || 1;

    $.each(poll.options, function(option, count) {
        const percent = ((count / totalVotes) * 100).toFixed(0);

        const itemHtml = `
              <div class="result-item mb-4">
                <div class="flex justify-between mb-1 text-gray-700 font-medium">
                  <span>${option}</span>
                  <span>${count}</span>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-4">
                  <div class="bg-blue-600 h-4 rounded-full transition-all duration-700 ease-out" style="width:0%"></div>
                </div>
              </div>
            `;
        resultDiv.append(itemHtml);

        // Select the inner bar
        const $bar = resultDiv.find('.result-item:last-child .bg-blue-600')[0];

        // Trigger animation on next browser frame
        requestAnimationFrame(() => {
          $bar.style.width = percent + '%';
        });
    });
}

$(document).ready(function() {
    loadPoll();

    // Initially disable the button
    $('#vote-btn').prop('disabled', true)
        .addClass('opacity-50 cursor-not-allowed');

    // Enable when an option is selected
    $(document).on('change', 'input[name="vote"]', function() {
        $('#vote-btn')
          .prop('disabled', false)
          .removeClass('opacity-50 cursor-not-allowed');
    });


    $('#vote-btn').click(function() {
        const choice = $('input[name="vote"]:checked').val();
        if (!choice) return alert('Selectati o optiune!');
        $.post(`/api/poll/${pollId}/vote?option=${encodeURIComponent(choice)}`, function(updatedPoll) {
            showResults(updatedPoll);
        }).fail(err => console.error('Vote error:', err));
    });
});