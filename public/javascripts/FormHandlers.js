'use strict';

function submitInBackground(formSelector, submitURL, reset = true, extraData = [],
                            errorMessage = "There was a problem submitting your form") {
    $(formSelector).submit(function (e) {

        // Prevent the form from clearing
        e.preventDefault();

        // TODO: I learned about this in jQuery
        let formData = $(this).serializeArray();

        console.log(formData);

        extraData.forEach((item) => {
            formData.push(item);
        });

        $.ajax({
            method: "post",
            url: submitURL,
            data: formData,
            dataType: "json",
            success: function (responseData, textStatus, jqXHR) {

                console.log(responseData);

                // Clear the text input
                if (responseData['success'] === true) {
                    if (reset === true) {
                        document.querySelector(formSelector).reset();
                    }
                } else {
                    console.log(errorMessage);
                }
            }
        });
    })
}


function submitWithGeo(formSelector, submitURL) {
    $(formSelector).on("submit", function (e) {

        // Prevent the form from clearing
        e.preventDefault();

        // Post the form data to the server
        function sendForm(extraData = []) {

            let formData = $(formSelector).serializeArray();

            extraData.forEach((item) => {
                formData.push(item);
            });

            $.ajax({
                method: "post",
                url: submitURL,
                data: formData,
                dataType: "json",
                success: function (responseData, textStatus, jqXHR) {

                    console.log(responseData);

                    // Clear the text input
                    if (responseData['success'] === true) {
                        document.querySelector(formSelector).reset();
                    } else {
                        console.log("There was an problem saving your journal entry")
                    }
                }
            });
        }

        // Options for the GeoLocation lookup
        let options = {
            "maximumAge": 10 * 1000
        };

        // Callback when user position is obtained
        function geoSuccess(position) {

            const extraData = [
                {name: "longitude", value: position.coords.longitude},
                {name: "latitude", value: position.coords.latitude}
            ];

            sendForm(extraData);
        }

        // Callback when user position cannot be obtained
        function geoFailure(error) {

            console.log("Failed to acquire user position");

            sendForm();
        }

        // Get the user's location at time of submission
        navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, options);
    });
}