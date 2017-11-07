'use strict';


// Handler for customized form submission
function submitInBackground(formSelector, submitURL, successCallback = () => null, extraData = []) {

    let formElem = $(formSelector);

    formElem.submit(function (e) {

        // Prevent the form from clearing
        e.preventDefault();

        // Get form data
        let formData = $(this).serializeArray();

        // Add extra data fields to the form
        extraData.forEach((item) => {
            formData.push(item);
        });

        // Submit the form asynchronously
        $.ajax({
            method: "post",
            url: submitURL,
            data: formData,
            dataType: "json",
            success: function (responseData, textStatus, jqXHR) {
                successCallback(responseData, formData, formElem);
            }
        });
    })
}


function submitWithGeo(formSelector, submitURL, successCallback = () => {
}) {

    let formElem = $(formSelector);

    formElem.on("submit", function (e) {

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

                    successCallback(responseData, formData, formElem);
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

/**
 * Submit JSON data asynchronously, passing response to a callback
 *
 * @param data
 * @param submitURL
 * @param successCallback
 * @param extraData
 */
function submitDataInBackground(data, submitURL, successCallback = () => null) {

    // Get form data
    let dataCopy = JSON.parse(JSON.stringify(data));

    // Submit the data asynchronously
    $.ajax({
        method: "post",
        url: submitURL,
        data: dataCopy,
        dataType: "json",
        success: function (responseData, textStatus, jqXHR) {
            successCallback(responseData);
        }
    });

}

function submitDataWithGeo(data, submitURL, successCallback = () => {
}) {


    let dataCopy = JSON.parse(JSON.stringify(data));

    // Post the form data to the server
    function sendData(lon, lat) {

        dataCopy.longitude = lon;
        dataCopy.latitude = lat;

        $.ajax({
            method: "post",
            url: submitURL,
            data: dataCopy,
            dataType: "json",
            success: function (responseData, textStatus, jqXHR) {

                successCallback(responseData);
            }
        });
    }

    // Options for the GeoLocation lookup
    let options = {
        "maximumAge": 10 * 1000
    };

    // Callback when user position is obtained
    const geoSuccess = (position) => {

        sendData(position.coords.longitude, position.coords.latitude);
    };

    // Callback when user position cannot be obtained
    const geoFailure = (error) => {

        console.log("Failed to acquire user position");

        sendData(0, 0);
    };

    // Get the user's location at time of submission
    navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, options);

}