'use strict';

/**
 * A class to manage and control subject list widgets.
 *
 * @param elementId
 * @param subjects
 * @constructor
 */
function SubjectList(elementId, subjects) {


    // The element Id containing the subject list
    this.elementId = elementId;

    // The list of subjects
    this.subjects = subjects;


    /**
     * Display the subject list
     *
     * @param subTotalMap
     */
    this.display = function fillSubjectList(subTotalMap) {

        const subjectName = (subject, total) => "<div class='subject-list-name'>" + subject['name'] + subjectTotal(total) + "</div>";

        const subjectTotal = (total) => "<span class='subject-list-total'> (" + total.toFixed(2).toString() + " hrs)</span>";

        const subjectDescription = (subject) => "<div class='subject-list-description'>" + subject['description'] + "</div>";

        const subjectOptions = "<td class='subject-list-options'>" + "<span><u>&middot;&middot;&middot;</u></span>" + "</td>";

        // Populate the subject list
        const subjectListHTML = this.subjects.map(function (subject, i, arr) {

            let total = 0;

            if (subTotalMap.has(subject.name)) {
                total = subTotalMap.get(subject.name);
            }

            return "<tr><td class='subject-list-element'>" + subjectName(subject, total) + subjectDescription(subject) + "</td>" + subjectOptions + "</tr>"
        });

        document.getElementById(this.elementId).innerHTML = "<table><colgroup><col><col></colgroup>" + subjectListHTML.join("") + "</table>";
    };


    /**
     * Sort the subject list by total hours per subject
     *
     * @param subTotalMap
     * @param descending
     */
    this.sortByHours = function (subTotalMap, descending = true) {

        const cmp = (sub1, sub2) => {

            let sub1hours = 0;
            let sub2hours = 0;

            if (subTotalMap.has(sub1.name)) {
                sub1hours = subTotalMap.get(sub1.name);
            }

            if (subTotalMap.has(sub2.name)) {
                sub2hours = subTotalMap.get(sub2.name);
            }

            if (descending) {
                return sub2hours - sub1hours;
            } else {
                return sub1hours - sub2hours;
            }
        };

        this.subjects.sort(cmp);
        this.display(subTotalMap);
    };

}
