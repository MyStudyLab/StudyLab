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
 * @param {string} elementId -
 * @member {int} intervalID -
 * @member {int} startTime - The time that this stopwatch begins (milliseconds since the epoch)
 * @constructor
 */
function Stopwatch(elementID) {

    this.elementID = elementID;
    this.intervalID = -1;
    this.startTime = -1;

    this.start = function () {

        this.startTime = Date.now();

        this.intervalID = setInterval(function () {
            document.getElementById(elementID).innerText = stopwatch.display();
        }, 1000);

    };

    this.startFrom = function (startTime) {

        this.startTime = startTime;

        //let elementId = this.elementID;

        this.intervalID = setInterval(function () {
            document.getElementById(elementID).innerText = stopwatch.display();
        }, 1000,);

    };

    this.reset = function () {
        document.getElementById(this.elementID).innerText = "00:00:00";
        clearInterval(this.intervalID);
    };

    this.displayZero = function () {
        document.getElementById(this.elementID).innerText = "00:00:00";
    };

    this.display = function () {

        const elapsed = Math.round((Date.now() - this.startTime) / 1000);

        const hours = Math.floor(elapsed / 3600);

        const minutes = Math.floor((elapsed % 3600) / 60);

        const seconds = elapsed % 60;

        const fields = [hours.toString(), minutes.toString(), seconds.toString()];

        const paddedFields = fields.map(function (curr, i, arr) {
            return padString(curr, "0", 2);
        });

        return paddedFields.join(":");
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