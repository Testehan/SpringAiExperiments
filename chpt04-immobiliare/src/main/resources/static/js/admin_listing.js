$(document).ready(function(){

    $('#clearButton').on('click', function () {
      $('#listingName').val('');
      $('#listingCity').val('');
      $('#listingType').val('');
      $('#listingMinPrice').val('');
      $('#listingMaxPrice').val('');
      $('#listingFilterForm').submit();
    });


})

