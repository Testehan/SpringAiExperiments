let imagesCount=0;

// input validation
const Joi = window.joi;
console.log(typeof Joi);
const schema = Joi.object({
    name: Joi.string().required().label('Title'),
    area: Joi.string().required().label('Area / Neighbourhood'),
    description: Joi.string().required().label('Description'),
    price: Joi.number().positive().required().label('Price'),
    surface: Joi.number().positive().required().label('Surface'),
    noOfRooms: Joi.number().positive().required().label('Number of rooms'),
    contact: Joi.alternatives().try(
                //Joi.string().email({ tlds: { allow: false } }),      // either an email
                Joi.string().regex(/^\d{9,10}$/).message('Phone number must be 9 or 10 digits long')
           ).required().label('Contact')
});

$(document).ready(function(){
    $("div[id^='divImage']").each(function(index){
        imagesCount++;
    });

    $("input[name='apartmentImages']").each(function(index){
        $(this).change(function(){                       // "this" is here an input type element with name image
             if (!checkFileSize(this)){
                return ;
             }
            showImageThumbnail(this, index);
        });
    });

    $("a[name='linkRemoveImage'").each(function(index){
        $(this).click(function(){
            removeImage(index);
        });
    });

    $("#addForm").submit(function(event) {
        handleSubmit(event);
    });

});

async function handleSubmit(event){
    event.preventDefault();

    var valid = await isFormValid();

    if (valid){
        const formData = new FormData(event.target);

        $.ajax({
            url: "/api/apartments/save",
            type: "POST",
            data: formData,
            processData: false, // Prevent jQuery from processing the FormData
            contentType: false, // Let the browser set the content type (multipart/form-data)
            success: function (response) {
                Toastify({
                    text: response,
                    duration: 4000,
                    style: {
                      background: "linear-gradient(to right, #007bff, #3a86ff)",
                      color: "white"
                    }
                }).showToast();

                $("#addForm")[0].reset();
                imagesCount = 0;
                $("#apartmentImages").empty();
                addExtraImageSection(0);
            },
            error: function (xhr) {
                console.log(xhr);

                Toastify({
                    text: xhr.responseText,
                    duration: 3000,
                    gravity: "top",
                    position: "right",
                    style: {
                      background: "linear-gradient(to right, #fd0713, #ff7675)",
                      color: "white"
                    }
                }).showToast();
            }
        })
    }

}

async function isFormValid(){

     // Extract form data that needs validation
    const formData = {
        name: $('#name').val(),
        area: $('#area').val(),
        description: $('#description').val(),
        price: $('#price').val(),
        surface: $('#surface').val(),
        noOfRooms: $('#noOfRooms').val(),
        contact: $('#contact').val(),
    };

    const { error } = schema.validate(formData, { abortEarly: false });
    if (error) {

        // Display validation errors
        const errorMessages = error.details.slice().reverse().forEach(err => {
            Toastify({
                text: err.message,
                duration: 5000,
                style: {
                    background: "linear-gradient(to right, #fd0713, #ff7675)",
                    color: "white"
                  }
            }).showToast();
        })
        return false;
    }

    const isValid = await isPhoneValid($('#contact').val());
    if (!isValid){
         Toastify({
            text: "Phone number is not valid",
            duration: 5000,
            style: {
                background: "linear-gradient(to right, #fd0713, #ff7675)",
                color: "white"
              }
        }).showToast();
        return false;
    }

    return isValid && !error;
}

async function isPhoneValid(phoneNumber){
    return new Promise((resolve, reject) => {
        $.ajax({
            url: APP_URL + "/api/apartments/validate/" + phoneNumber ,
            type: "GET",
            success: function (response) {
                resolve(response);
            },
            error: function () {
                 Toastify({
                    text: "Error checking phone number.",
                    duration: 5000,
                    style: {
                        background: "linear-gradient(to right, #fd0713, #ff7675)",
                        color: "white"
                      }
                }).showToast();
                reject(false);
            }
        });
    });

}

function showImageThumbnail(fileInput, index){
    var file = fileInput.files[0];
    fileName = file.name;

    var reader = new FileReader();
    reader.onload = function(e){
        $("#newImageThumbnail"+index).attr("src", e.target.result)
    }

    reader.readAsDataURL(file);

//    if (index >= imagesCount-1){
        addExtraImageSection(index + 1);
//    }
}

function addExtraImageSection(index){
    // we need to increase index by 1 because index starts at 0, however in the UI you don't want the user to see text starting
    // from 0, like "Extra image 0"...so while the id of the elements can have 0, the other index locations are incremented.

    imagesCount++;
    htmlExtraImage = `
        <div class="col border m-3 p-2" id="divImage${imagesCount}">
           <div id="imageHeader${index}"><label>` + LABEL_IMAGE_NUMBER + ` ${imagesCount}</label></div>
           <div class="">
               <img id="newImageThumbnail${index}" alt="Image ${imagesCount} preview" class="img-fluid"
                    src="${defaultThumbnailImageSrc}" />
           </div>
           <div>
               <input type="file" name="apartmentImages"
                    onChange="showImageThumbnail(this, ${index})"
                    accept="image/png, image/jpeg" />
           </div>
       </div>
    `;

    htmlLinkRemove=`
        <a class="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
            name='linkRemoveImage'
            href="javascript:removeImage(${imagesCount-1})"
            title="Remove this image">` + BUTTON_DELETE_IMAGE + `</a>
    `;

    $("#apartmentImages").append(htmlExtraImage);

    var divImagePrevious = $("div[id^='divImage']").eq(-2); // second to last divimage
    var elementWithSpecificName = divImagePrevious.find('[name="linkRemoveImage"]');

    if (elementWithSpecificName.length > 0) {

    } else {
        // we select the previous image section in order to add the remove button
        divImagePrevious.append(htmlLinkRemove);
    }
}

function removeImage(index){
   // index--;        // because apparently these indexes start from 1 and my divs from 0
    imagesCount = imagesCount - 2;      // because we remove both the current and last element
    $("#divImage"+index).remove();
    const lastElement = $("div[id^='divImage']").last();    /// we need to remove the last elemet and recreate it with a correct div number id
    lastElement.remove();
    addExtraImageSection(index);
}

function checkFileSize(fileInput){
    MAX_FILE_SIZE=5000000           // 5 MB

    fileSize = fileInput.files[0].size;

    if (fileSize > MAX_FILE_SIZE){      // configured in various places depending on the needs
        fileInput.setCustomValidity("You must choose a file with a size LESS than " + (MAX_FILE_SIZE/1000000) + " MB");
        fileInput.reportValidity();
        return false;
    } else {
        fileInput.setCustomValidity("");
        return true;
    }

}
