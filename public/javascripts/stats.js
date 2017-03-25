// TODO: Probability distribution of session length (total and per-subject)
// TODO: Probability distribution of daily total (total and per-subject)

// TODO: Should simply use a library stdev function on session duration input
function stdevOfSessionLength(sessions) {

    const mu = averageSessionDuration(sessions);

    const sse = sessions.reduce(function (acc, curr, ind) {
        return acc + Math.pow(durationInHours(curr) - mu, 2);
    });

    return Math.pow(sse / (sessions.length - 1), 0.5);
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


function moving_average(sessions, radius, n) {

    const s = sessions[0].startTime + radius;
    const e = sessions[sessions.length - 1].endTime;

    const diff = (e - s) / n;

    let res = [];
    let t = 0;
    let p = s;

    // this loop is wrong. when window overlaps occur, a session
    // should be included in both
    sessions.forEach(function (curr, i, arr) {

        // Will split sessions here in final version
        if (curr.endTime <= p) {
            t += curr.endTime - curr.startTime;
        } else {
            res.push([new Date(p), t / (3600 * 1000)]);
            t = curr.endTime - curr.startTime;
            p += diff;
        }
    });

    return res;
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

function averageSessionDuration(sessions) {

    const total = sessions.reduce(function (acc, curr, ind) {
        return acc + durationInHours(curr)
    }, 0);

    return total / sessions.length;
}