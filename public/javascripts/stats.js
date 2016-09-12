// TODO: Comment this function and split sessions on boundaries
function stats1(sessions, numLevels) {

    var diff;
    var cumul = 0;

    // Total duration of completed sessions
    var total = sessions.reduce(function (prev, curr, arr) {
        return prev + ((curr.endTime - curr.startTime) / (3600 * 1000));
    }, 0);


    var levelSize = Math.ceil(total / numLevels);

    var level = 1;
    var res = [[new Date(sessions[0].startTime), 0]];

    for (var i = 0; i < sessions.length; i++) {
        diff = (sessions[i].endTime - sessions[i].startTime) / (3600 * 1000);

        cumul += diff;

        if (cumul >= level * levelSize) {
            res.push([new Date(sessions[i].endTime), cumul]);
            level += 1;
        }
    }

    // Last item in cums
    if (res.length < numLevels + 1) {
        res.push([new Date(sessions[sessions.length - 1].endTime), cumul]);
    }


    return {
        "cumulative": res,
        "total": total
    }
}


function stats2(sessions) {

    var diff = 0;
    var total = 0;
    var count = 0;
    var subTotals = new Map();
    var counts = new Map();
    var diffs = [];

    sessions.forEach(function (curr, i, arr) {

        diff = (curr.endTime - curr.startTime) / (3600 * 1000);

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

    var a = Array.from(subTotals.entries());

    return {
        "subjectTotals": a.sort(cmp).slice(0, 10)
    }
}


function moving_average(sessions, radius, n) {

    s = sessions[0].startTime + radius;
    e = sessions[sessions.length - 1].endTime;

    diff = (e - s) / n;

    res = [];
    t = 0;
    p = s;

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

// Compute the total duration, in seconds, of a group of sessions.
function sumSessions(sessions) {

    var total = 0;

    sessions.forEach(function (curr, i, arr) {
        total += curr.endTime - curr.startTime;
    });

    return total / (3600 * 1000);
}


// Assume sessions are sorted chronologically and non-overlapping.
// TODO: Check that we are not modifying the original sessions array
function sessionsSince(t, sessions) {

    var result = [];

    for (var i = sessions.length - 1; i >= 0; i--) {

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

function todaysSessions(sessions) {

}