// TODO: Probability distribution of session length (total and per-subject)
// TODO: Probability distribution of daily total (total and per-subject)

/*
 * Return the total duration of a sequence of sessions (hours).
 */
function sumSessions(sessions) {

    return sessions.reduce(function (acc, curr, i) {
        return acc + ((curr.stop - curr.start) / 3600000);
    }, 0);

}

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

// TODO: Should simply use a library stdev function on session duration input
function stdevOfSessionLength(sessions) {

    const mu = averageSessionDuration(sessions);

    const sse = sessions.reduce(function (acc, curr, ind) {
        return acc + Math.pow(durationInHours(curr) - mu, 2);
    });

    return Math.pow(sse / (sessions.length - 1), 0.5);
}


/*
 * Return an array of moving averages with the given radius.
 *
 * TODO: Use stepSize to determine how many days separate consecutive data points
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


// Compute a list of cumulative study totals
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

// TODO: Consolidate with the other stats function
function stats2(sessions) {

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


    function cmp(sub1, sub2) {
        if (sub1[1] < sub2[1]) {
            return 1;
        }

        if (sub1[1] > sub2[1]) {
            return -1;
        }

        return 0;
    }

    return {
        "subjectTotals": Array.from(subTotals.entries()).sort(cmp).slice(0, 10)
    }
}

function durationInHours(session) {
    return (session.endTime - session.startTime) / 3600000;
}

// Compute the total duration, in seconds, of a group of sessions.
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

// TODO: Use a mean function from a library
function averageSessionDuration(sessions) {

    const total = sessions.reduce(function (acc, curr, ind) {
        return acc + durationInHours(curr)
    }, 0);

    return total / sessions.length;
}