'use strict';

/**
 * Pad a string up to a given length
 *
 * @param {string} str
 * @param {string} padStr
 * @param {int} len
 * @return {string}
 */
function padString(str, padStr, len) {

    const fillLength = Math.max(len - str.length, 0);

    const numRepeats = Math.floor(fillLength / padStr.length);
    const remainder = fillLength - numRepeats * padStr.length;

    return padStr.repeat(numRepeats) + padStr.substr(0, remainder) + str;
}

/**
 * A simple stopwatch
 *
 * @param {string} elementID - The ID of the element that contains the clock
 * @member {int} intervalID - The value returned by setInterval
 * @member {int} startTime - The time that this stopwatch begins (milliseconds since the epoch)
 * @constructor
 */
function StudyClock(elementID) {

    // Initialize members
    this.elementID = elementID;
    this.intervalID = -1;
    this.startTime = -1;


    /**
     * Start the clock (at the current time)
     *
     * @param givenStartTime {number} - The time at which this clock began (milliseconds since epoch)
     *
     */
    this.start = function (givenStartTime = 0) {

        // Check if the clock is already running
        if (this.startTime > -1) {
            throw new Error("Can't start clock when already active");
        }

        if (givenStartTime > 0) {
            this.startTime = givenStartTime;
        } else {
            this.startTime = Date.now();
        }

        this.display();

        this.intervalID = setInterval(() => this.display(), 1000);
    };


    /**
     * Reset the clock
     *
     */
    this.reset = function () {

        // Clear the current interval
        clearInterval(this.intervalID);
        this.intervalID = -1;

        // Reset the start time
        this.startTime = -1;

        // Display the now inactive clock
        this.display();
    };


    // Display the elapsed time as hh:mm:ss
    this.display = function () {

        // Display all zeros when the clock is inactive
        if (this.startTime < 0) {
            document.getElementById(this.elementID).innerHTML = "00:00:00";
            return;
        }

        let elapsed = Math.round((Date.now() - this.startTime) / 1000);

        const hours = Math.floor(elapsed / 3600);
        const minutes = Math.floor((elapsed % 3600) / 60);
        const seconds = elapsed % 60;

        const fields = [hours.toString(), minutes.toString(), seconds.toString()];

        const paddedFields = fields.map(function (curr, i, arr) {
            return padString(curr, "0", 2);
        });

        // Indicate which digits are inactive
        let i = 0;

        // Run through the inactive digits
        for (; i < paddedFields.length; i++) {

            if (paddedFields[i] === "00") {

            } else if (paddedFields[i].substr(0, 1) === "0") {
                paddedFields[i] = paddedFields[i].substr(0, 1) + '<span class="clock-active-digit">' + paddedFields[i].substr(1) + '</span>';
                break;
            } else {
                paddedFields[i] = '<span class="clock-active-digit">' + paddedFields[i] + '</span>';
                break;
            }
        }

        // Indicate the active digits
        for (i++; i < paddedFields.length; i++) {
            paddedFields[i] = '<span class="clock-active-digit">' + paddedFields[i] + '</span>';
        }

        document.getElementById(this.elementID).innerHTML = paddedFields.join('<span class="clock-separator">:</span>');
    };
}