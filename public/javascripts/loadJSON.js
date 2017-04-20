'use strict';


/**
 * Load a resource and pass it to the given function
 *
 * @param path {string} - The path of the resource on the server
 * @param callback {function} - A function to call with the response
 */
function loadJSONFile(path, callback) {

    var request = new XMLHttpRequest();

    // Specify what to do with the response
    request.onreadystatechange = function () {
        if (request.readyState === 4) {
            if (request.status === 200) {

                // Parse the response text as JSON
                let data = JSON.parse(request.responseText);

                // Pass the data to the callback function
                if (callback) {
                    callback(data);
                }
            }
        }
    };

    // We are sending an HTTP GET request
    request.open('GET', path);

    // Send the request
    request.send();
}