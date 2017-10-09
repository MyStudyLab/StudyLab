function JournalEntryList(elementId, entries) {


    // The element Id containing the list
    this.elementId = elementId;

    // The list of journal entries
    this.entries = entries;

    // Display the entry list in its current state
    this.display = function () {

        let entryHtml = this.entries.map((entry) => {

            let text = '<div class="journal-entry-text partial-border center-text-content">' + '<p>' + entry.text + '</p>';

            let date = '<div class="journal-entry-info">' + moment(entry.timestamp).format('YYYY-MM-DD HH:mm') + '</div>' + '</div>';

            return text + date;
        });

        document.getElementById(this.elementId).innerHTML = entryHtml.join("");
    };

    //
    this.sort = function (oldestFirst = false) {

        const factor = (oldestFirst === true) ? -1 : 1;

        this.entries.sort((a, b) => {

            return factor * Math.sign(b.timestamp - a.timestamp);
        });
    }

}