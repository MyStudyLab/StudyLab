'use strict';

/**
 * A searchable list of journal entries
 *
 * @param elementId
 * @param entries
 * @constructor
 */
function JournalEntryList(elementId, entries) {

    // The element Id containing the list
    this.elementId = elementId;

    // The list of journal entries
    this.entries = entries;

    // The list entries currently displayed
    this.resultSet = entries;

    // The containing element for this searchable list
    const elem = document.getElementById(elementId);

    // The search bar
    elem.innerHTML = `<div id="journal-search-bar">
                         <form id="journal-search-form">

                           <label for="journal-search-text" class="sr-only">Search</label>
                           <input type="text" name="search-text" id="journal-search-text" class="form-control" autocomplete="off">

                            <!--
                           <label class="sr-only">Near Me</label>
                           <input type="checkbox" name="near-me" class="form-control">
                        -->
                         </form>
                       </div>`;


    // The containing element for the list
    const entryContainer = document.createElement('div');
    elem.append(entryContainer);

    /**
     * Display the entry list in its current state
     */
    this.display = () => {

        // Clear the container before displaying
        entryContainer.innerHTML = "";

        let entryHtml = this.resultSet.map((entry) => {

            return `<div class='journal-entry-text partial-border center-text-content'>
                      <p>${entry.text}</p>
                      <div class="journal-entry-info">${moment(entry.timestamp).format('YYYY-MM-DD HH:mm')}</div>
                    </div>`;
        });

        entryContainer.innerHTML = entryHtml.join("");
    };


    /**
     * Sort the entries by timestamp
     *
     * @param oldestFirst
     */
    this.sort = function (oldestFirst = false) {

        const factor = (oldestFirst === true) ? -1 : 1;

        this.resultSet.sort((a, b) => {

            return factor * Math.sign(b.timestamp - a.timestamp);
        });
    };


    /**
     * Find all entries containing a search term
     *
     * @param rawSearchTerm
     */
    this.filter = function (rawSearchTerm) {

        // Ignore case when searching
        const searchTerm = rawSearchTerm.toLowerCase();

        this.resultSet = this.entries.filter(function (elem) {
            return elem.text.toLowerCase().includes(searchTerm);
        });
    };


    /**
     * Find all entries near the given coordinates
     *
     * @param coords
     * @param radius
     */
    this.near = function (coords, radius) {

        this.resultSet = this.entries.filter(function (elem) {

            // TODO: call haversine function
            return 0;
        });

    };

    // Set up the handler for the journal search bar
    const searchText = $('#journal-search-text');

    searchText.keyup(() => {
        const searchTerm = searchText.val();

        this.filter(searchTerm);
        this.display();
    });

    // The initial display
    this.sort();
    this.display();

}