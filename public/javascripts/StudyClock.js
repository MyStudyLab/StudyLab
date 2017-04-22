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
 * @param {string} elementID -
 * @member {int} intervalID -
 * @member {int} startTime - The time that this stopwatch begins (milliseconds since the epoch)
 * @constructor
 */
function Stopwatch(elementID) {

    // Initialize members
    this.elementID = elementID;
    this.intervalID = -1;
    this.startTime = -1;

    // Start the stopwatch
    this.start = function () {

        // Only start if currently stopped
        if (this.startTime < 0) {

            this.startTime = Date.now();

            this.intervalID = setInterval(function () {
                document.getElementById(elementID).innerHTML = stopwatch.display();
            }, 1000);
        }

    };

    // Run the stopwatch, using the given time as the start
    this.startFrom = function (startTime) {

        if (startTime < 0) {
            throw new Error("start time must be non-negative");
        }

        // Only start if currently stopped
        if (this.startTime < 0) {

            this.startTime = startTime;

            this.display(startTime);

            this.intervalID = setInterval(function () {
                document.getElementById(elementID).innerHTML = stopwatch.display();
            }, 1000,);
        }

    };

    // Reset the stopwatch
    this.reset = function () {
        this.displayZero();
        clearInterval(this.intervalID);
        this.startTime = -1;
    };

    // Display all zeros on the stopwatch
    this.displayZero = function () {
        document.getElementById(this.elementID).innerHTML = this.display(0);
    };

    // Display the elapsed time as hh:mm:ss
    this.display = function (elapsedGiven = -1) {

        let elapsed = elapsedGiven;

        if (elapsed < 0) {
            elapsed = Math.round((Date.now() - this.startTime) / 1000);
        }

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

        return paddedFields.join('<span class="clock-separator">:</span>');
    };
}

/**
 * A simple clock
 *
 * @param {int} elapsed - The number of seconds already elapsed
 * @constructor
 */
function Clock(elapsed = 0) {

    this.hours = Math.floor(elapsed / 3600);

    this.minutes = Math.floor((elapsed % 3600) / 60);

    this.seconds = elapsed % 60;

    this.set = function (elapsed) {
        this.hours = Math.floor(elapsed / 3600);

        this.minutes = Math.floor((elapsed % 3600) / 60);

        this.seconds = elapsed % 60;
    };

    this.reset = function () {
        this.seconds = 0;
        this.minutes = 0;
        this.hours = 0;
    };

    this.tick = function () {
        this.seconds++;

        if (this.seconds > 59) {
            this.minutes++;
            this.seconds = 0;

            if (this.minutes > 59) {
                this.hours++;
                this.minutes = 0;
            }
        }


    };

    this.toString = function () {

        const fields = [this.hours.toString(), this.minutes.toString(), this.seconds.toString()];

        const paddedFields = fields.map(function (curr, i, arr) {
            return padString(curr, "0", 2);
        });

        return paddedFields.join(":");
    }
}