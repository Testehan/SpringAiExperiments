var imagesCount=0;

$(document).ready(function(){

    $("input[name='apartmentImages']").each(function(index){
            imagesCount++;
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

});

function showImageThumbnail(fileInput, index){
    var file = fileInput.files[0];
    fileName = file.name;
//    imageNameHiddenField = $("#imageName"+index)
//    if (imageNameHiddenField.length > 0){
//        imageNameHiddenField.val(fileName);
//    }

    var reader = new FileReader();
    reader.onload = function(e){
        $("#newImageThumbnail"+index).attr("src", e.target.result)
    }

    reader.readAsDataURL(file);

    if (index >= imagesCount-1){
        addExtraImageSection(index + 1);
    }
}

function addExtraImageSection(index){
    // we need to increase index by 1 because index starts at 0, however in the UI you don't want the user to see text starting
    // from 0, like "Extra image 0"...so while the id of the elements can have 0, the other index locations are incremented.
    htmlExtraImage = `
        <div class="col border m-3 p-2" id="divImage${index}">
           <div id="imageHeader${index}"><label>Image no ${index + 1}</label></div>
           <div class="">
               <img id="newImageThumbnail${index}" alt="Image ${index + 1} preview" class="img-fluid"
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
            href="javascript:removeImage(${index})"
            title="Remove this image">Delete</a>
    `;

    $("#apartmentImages").append(htmlExtraImage);

    var divImagePrevious = $("#divImage" + (index-1));
    var elementWithSpecificName = divImagePrevious.find('[name="linkRemoveImage"]');

    if (elementWithSpecificName.length > 0) {
      var divImageCurrent= $("#divImage" + index);
       divImageCurrent.append(htmlLinkRemove);
    } else {
        // we select the previous image section in order to add the remove button
        divImagePrevious.append(htmlLinkRemove);
    }



    imagesCount++;
}

function removeImage(index){
    $("#divImage"+index).remove();
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