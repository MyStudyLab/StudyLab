'use strict';

// TODO: Probability distribution of session length (total and per-subject)
// TODO: Probability distribution of daily total (total and per-subject)


function probabilityOverlap(user1, user2) {

    let min_sequence = [];

    if (user1.length != user2.length) {
        //todo
    }

    for (let i = 0; i < user1.length(); i++) {
        min_sequence.append(Math.min(user1, user2))
    }

    return min_sequence;
}


/**
 * Return the total duration of a sequence of sessions (hours).
 *
 * @param {Array} sessions  - An array of unprocessed study sessions
 * @returns {number}
 */
function sumSessions(sessions) {

    return sessions.reduce(function (acc, curr, i) {
        return acc + ((curr.stop - curr.start) / 3600000);
    }, 0);
}


// TODO: Rewrite this once the user join time is sent with the study data
/*
 * Return the number of days since the first session.
 */
function daysSinceStart(dayGroups) {

    return dayGroups.length;
}


/*
 * Return the total duration of today's sessions
 */
function todaysTotal(dayGroups) {
    return sumSessions(dayGroups[dayGroups.length - 1]['sessions']);
}

/*
 * Return the sessions for the current day.
 */
function todaysSessions(dayGroups) {
    return dayGroups[dayGroups.length - 1]['sessions'];
}


/**
 * Return the average value of a numeric array
 *
 * @param {Array} numArr - An array of numbers
 * @returns {number}
 */
function avg(numArr) {

    const total = numArr.reduce(function (acc, curr, ind) {
        return acc + curr
    }, 0);

    return total / numArr.length;
}


/**
 * Return the standard deviation of a numeric array
 *
 * @param {Array} numArr - An array of numbers
 * @returns {number}
 */
function stdDev(numArr) {

    const mu = avg(numArr);

    const sse = numArr.reduce(function (acc, curr, ind) {
        return acc + Math.pow(curr - mu, 2);
    });

    return Math.pow(sse / (numArr.length - 1), 0.5);
}


/**
 * Return the standard deviation of session length
 *
 * @param {Array} sessions - An array of unprocessed study sessions
 * @returns {number}
 */
function stdevOfSessionLength(sessions) {

    let durations = durationInHours(sessions);

    return stdDev(durations);
}


/*
 * Return an array of moving averages with the given radius.
 *
 */
function movingAverage(dayGroups, radius, stepSize) {

    if (dayGroups.length < radius) {
        return [];
    }

    const dailyTotals = dayGroups.map(function (curr, i, arr) {
        return {
            "date": curr['date'],
            "total": sumSessions(curr['sessions'])
        }
    });

    // Sum of study time for the window being analyzed
    let windowTotal = sumArray(dailyTotals.slice(0, radius).map(function (curr, i, arr) {
        return curr['total'];
    }));

    let res = [[dayGroups[radius - 1]['date'].valueOf(), windowTotal / radius]];

    for (let i = radius; i < dayGroups.length; i++) {
        windowTotal -= dailyTotals[i - radius]['total'];
        windowTotal += dailyTotals[i]['total'];
        res.push([dailyTotals[i]['date'].valueOf(), windowTotal / radius]);
    }

    return res;
}


/**
 * Group sessions by day
 *
 * Does not handle sessions longer than 24 hours
 *
 * @param {Array} sessions - An array of unprocessed study sessions
 * @returns {Array}
 */
function splitDays(sessions) {

    // TODO: How should we generalize to allow users to change timezones?
    const tz = "America/Chicago";

    // Trivial case
    if (sessions.length === 0) {
        return [];
    }

    // Get an array of sessions that use moment objects as timestamps
    let m_sessions = convertTimestampsToMoments(sessions, tz);

    // The end of the day being handled
    let marker = m_sessions[0].start.clone().endOf('day');

    // The end of the current day
    const endOfToday = moment().tz(tz).endOf('day');

    // An array of objects to hold the sessions of successive day
    let days = [];

    while (marker <= endOfToday) {
        days.push({"date": marker.clone(), "sessions": []});
        marker.add(1, 'day');
    }

    // Day index
    let day_ind = 0;

    m_sessions.forEach(function (session, i, arr) {

        // Skip any empty days
        while (session.start > days[day_ind].date) {
            day_ind += 1;
        }

        // Add this session to the current day. Split if necessary.
        if (session.stop < days[day_ind].date) {
            days[day_ind].sessions.push(session)
        } else {

            // TODO: To fix the 24 hour bug, we would use some kind of loop here

            days[day_ind].sessions.push({
                "start": session.start.clone(),
                "stop": days[day_ind].date.clone(),
                "subject": session.subject
            });

            days[day_ind + 1].sessions.push({
                "start": days[day_ind].date.clone().add(1, 'day').startOf('day'),
                "stop": session.stop.clone(),
                "subject": session.subject
            });
        }
    });

    return days;
}


/**
 * Compute a list of cumulative study totals
 *
 * @param {Array} sessions - An array of unprocessed study sessions
 * @param numLevels - Controls the resolution of the result
 * @returns {[*]}
 */
function cumulative(sessions, numLevels) {

    // A running total of the hours studied
    let cumul = 0;

    // Total duration of completed sessions
    const total = sumRawSessions(sessions);

    const levelSize = total / numLevels;

    let level = 1;

    // List of timestamped cumulative totals
    let res = [[new Date(sessions[0].startTime), 0]];

    sessions.forEach(function (curr, i, arr) {

        // Add the session's duration to the cumulative total
        cumul += (curr.endTime - curr.startTime) / (3600 * 1000);

        if (cumul >= level * levelSize) {

            // How much the session extends past the level boundary
            const t = (cumul - level * levelSize) * 3600 * 1000;

            res.push([new Date(curr.endTime - t), level * levelSize]);
            level += 1;
        }
    });

    // Add the last item in cumuls if needed
    if (res.length < numLevels + 1) {
        res.push([new Date(sessions[sessions.length - 1].endTime), cumul]);
    }

    return res;
}

// Return the cumulative sequence, with a data point for every day
function denseCumulative(dayGroups) {

    let cumul = 0;

    let cumuls = [[dayGroups[0]['date'].clone().startOf('day').valueOf(), 0]];

    const dailyTotals = dayGroups.map(function (curr, i, arr) {
        return [curr['date'].valueOf(), sumSessions(curr['sessions'])];
    });

    dailyTotals.forEach(function (curr, i, arr) {

        cumul += curr[1];

        cumuls.push([curr[0], cumul]);

    });

    return cumuls;
}


/**
 * Return the total hours studied per subject
 *
 * @param {Array} sessions - An array of unprocessed study sessions
 * @returns {Map}
 */
function subjectTotals(sessions) {

    // The total number of hours studied
    let total = 0;

    // The total number of sessions logged
    let count = 0;

    // Keys are subject names, values are the times spent studying that subject
    let subTotals = new Map();

    // Keys are subject names, values are the number of sessions studied
    let counts = new Map();

    // A list of sessions durations
    let diffs = [];

    // Compute the time spent studying each subject
    sessions.forEach(function (curr, i, arr) {

        const diff = (curr.endTime - curr.startTime) / (3600 * 1000);

        if (subTotals.has(curr.subject)) {
            counts.set(curr.subject, counts.get(curr.subject) + 1);
            subTotals.set(curr.subject, subTotals.get(curr.subject) + diff);
        } else {
            counts.set(curr.subject, 1);
            subTotals.set(curr.subject, diff);
        }

        count += 1;
        total += diff;
        diffs.push(diff);
    });

    return subTotals;
}


function durationInHours(session) {
    return (session.endTime - session.startTime) / 3600000;
}


// Compute the total duration, in seconds, of a group of sessions.

/**
 *
 * @param {Array} sessions - An array of unprocessed study sessions
 * @returns {number}
 */
function sumRawSessions(sessions) {

    let total = 0;

    sessions.forEach(function (curr, i, arr) {
        total += curr.endTime - curr.startTime;
    });

    return total / (3600 * 1000);
}


// Assume sessions are sorted chronologically and non-overlapping.
// TODO: Check that we are not modifying the original sessions array
function sessionsSince(t, sessions) {

    let result = [];

    for (let i = sessions.length - 1; i >= 0; i--) {

        if (sessions[i].endTime > t) {
            result.push(sessions[i]);
        } else {
            break;
        }

    }

    // Handle a session that spans t
    if (result[0].startTime < t) {
        result[0].startTime = t;
    }

    // Need to reverse the result array
    result.reverse();

    return result;
}

/**
 * Return the average duration of a study session
 *
 * @param {Array} sessions - An array of unprocessed study sessions
 * @returns {number}
 */
function averageSessionDuration(sessions) {

    let durations = durationInHours(sessions);

    return avg(durations);
}


/**
 * Return the yearly totals.
 *
 * @param dayGroups
 * @returns {Map}
 */
function yearlyTotals(dayGroups) {

    // A map in which keys are years and values are the number of hours studied in a year
    let m = new Map();

    dayGroups.forEach(function (curr, i, arr) {

        const y = curr.date.year();

        if (m.has(y)) {
            m.set(y, m.get(y) + sumSessions(curr.sessions));
        } else {
            m.set(y, sumSessions(curr.sessions));
        }
    });

    return m;
}


/*
 * Compute the length of the current streak (in days)
 */
function currentStreak(dayGroups) {

    if (dayGroups.length == 0) {
        return 0;
    }

    let i = dayGroups.length - 2;
    let streak = 0;

    // Count days in the streak, not including today
    while (i >= 0 && dayGroups[i]['sessions'].length > 0) {
        i--;
        streak++;
    }

    // If the user has programmed today, add it to the streak
    if (dayGroups[dayGroups.length - 1]['sessions'].length > 0) {
        streak += 1;
    }

    return streak;
}


/**
 * Compute the average number of hours studied on each weekday
 *
 * @param dayGroups
 * @returns {Array}
 */
function dayOfWeekAverages(dayGroups) {

    const days = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
    let dayTotals = [0, 0, 0, 0, 0, 0, 0];
    let counts = [0, 0, 0, 0, 0, 0, 0];

    // Compute the total number and duration of sessions per day of week
    dayGroups.forEach(function (curr, i, arr) {
        dayTotals[curr.date.day()] += sumSessions(curr.sessions);
        counts[curr.date.day()] += 1;
    });

    return dayTotals.map(function (curr, i, arr) {
        return [days[i], dayTotals[i] /= counts[i]];
    });
}


/*
 * Compute a histogram of daily totals
 */
function dailyTotalHistogram(dailyTotals, numBins) {

    // Bins are as follows: <=0, <=1, <=2, ..., >=numBins - 1
    let bins = [];

    for (let i = 0; i < numBins; i++) {
        bins.push(0);
    }

    dailyTotals.forEach(function (curr, i, arr) {
        bins[Math.min(Math.ceil(curr), numBins - 1)] += 1;
    });

    // Normalize the bins
    bins.forEach(function (curr, i, arr) {
        bins[i] = bins[i] / dailyTotals.length;
    });

    return bins;
}


/**
 * Return the average-day probability vector
 *
 * @param {number} numBins
 * @param {Array} dayGroups
 * @returns {Array}
 */
function probability(numBins, dayGroups) {

    let bins = [];

    for (let i = 0; i < numBins; i++) {
        bins.push(0);
    }

    dayGroups.forEach(function (dayGroup, i, arr) {
        dayGroup['sessions'].forEach(function (session, j, arr) {

            const upperBound = dayGroup['date'].clone();
            const lowerBound = dayGroup['date'].clone().startOf('day');

            const startBin = Math.floor((session.start - lowerBound) * numBins / (upperBound - lowerBound));
            const stopBin = Math.floor((session.stop - lowerBound) * numBins / (upperBound - lowerBound));

            for (let b = startBin; b < stopBin; b++) {
                bins[b] += 1;
            }
        });
    });

    // Normalize
    bins.forEach(function (curr, i, arr) {
        bins[i] = curr / dayGroups.length;
    });

    return bins;
}


/**
 * Return the average-day probability vector
 *
 * The time of day in raw hours is returned as well
 *
 * @param {number} numBins
 * @param {Array} dayGroups
 * @returns {Array}
 */
function probabilityWithTime(numBins, dayGroups) {

    let bins = [];

    for (let i = 0; i < numBins; i++) {
        bins.push(0);
    }

    dayGroups.forEach(function (dayGroup, i, arr) {
        dayGroup['sessions'].forEach(function (session, j, arr) {

            const upperBound = dayGroup['date'].clone();
            const lowerBound = dayGroup['date'].clone().startOf('day');

            const startBin = Math.floor((session.start - lowerBound) * numBins / (upperBound - lowerBound));
            const stopBin = Math.floor((session.stop - lowerBound) * numBins / (upperBound - lowerBound));

            for (let b = startBin; b < stopBin; b++) {
                bins[b] += 1;
            }
        });
    });

    // Normalize
    bins.forEach(function (curr, i, arr) {
        bins[i] = curr / dayGroups.length;
    });

    return bins.map(function (curr, i, arr) {
        return [24 * (i / numBins), curr]
    });
}


// Convert a moment object to use moment.js moments for the start and end times
function convertTimestampsToMoments(sessions, tz) {
    return sessions.map(function (curr, i, arr) {
        return {
            "start": moment(curr.startTime).tz(tz),
            "stop": moment(curr.endTime).tz(tz),
            "subject": curr.subject
        }
    })
}


/*
 * Return the sum of an array.
 *
 * TODO: Find some library function to replace this.
 */
function sumArray(arr) {
    return arr.reduce(function (acc, curr, i) {
        return acc + curr;
    }, 0);
}


/*
 * Return the sessions occurring in the current month in chronological order.
 */
function currentMonthSessions(dayGroups) {

    // The moment at which the current month began.
    const startOfMonth = dayGroups[dayGroups.length - 1]['date'].clone().startOf('month');

    let i = dayGroups.length - 1;

    // Iterate in reverse until encountering a day not in this month
    while (i >= 0 && dayGroups[i]['date'] > startOfMonth) {
        i--;
    }

    let res = [];

    // The original array is not modified
    dayGroups.slice(i + 1).forEach(function (dayGroup, i, arr) {
        dayGroup['sessions'].forEach(function (curr, i, arr) {
            res.push(curr);
        });
    });

    return res;
}


/*
 * Filter day groups by the given day(s) of the week
 */
function filterByWeekday(dayGroups, daysOfWeek) {

    return dayGroups.filter(function (day, i, arr) {
        return daysOfWeek.includes(day.date.day());
    });
}


/*
 * Return the total hours studied each day
 */
function dailyTotals(dayGroups) {
    return dayGroups.map(function (curr, i, arr) {
        return sumSessions(curr.sessions);
    })
}


/*
 * Return the duration of a moment-based session.
 */
function duration(session) {
    return session.end - session.start / 3600000;
}


/*
 * Return the average of the daily totals.
 */
function dailyAverage(dayGroups) {

    return sumArray(dailyTotals(dayGroups)) / dayGroups.length;
}

/*
 * Return the standard deviation of the distribution of daily totals
 *
 * TODO: Use a library function
 */
function dailyStdev(dayGroups) {

    const mu = dailyAverage(dayGroups);

    const sse = dayGroups.reduce(function (acc, curr, ind) {
        return acc + Math.pow(sumSessions(curr.sessions) - mu, 2);
    }, 0);

    return Math.pow(sse / (dayGroups.length - 1), 0.5);
}


function sessionDuration(session) {
    return (session.stop - session.start) / 3600000;
}