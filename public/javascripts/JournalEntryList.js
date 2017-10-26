'use strict';


const toRad = (n) => {
    return n * Math.PI / 180;
};

const distance = (lon1, lat1, lon2, lat2) => {
    let R = 6371; // Radius of the earth in km
    let dLat = toRad(lat2 - lat1);  // Javascript functions in radians
    let dLon = toRad(lon2 - lon1);
    let a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    let c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    let d = R * c; // Distance in km
    return d * 1000; // Distance in m
};

/**
 * A searchable list of journal entries
 *
 * @param elementId The document ID of the containing HTML element
 * @param entries The list of journal entries to display
 * @constructor
 */
function JournalEntryList(elementId, entries) {

    // The element Id containing the list
    this.elementId = elementId;

    // The list of journal entries
    this.entries = entries.slice();
    this.entries.sort();
    this.entries.reverse();

    // The list entries currently displayed
    this.resultSet = this.entries.slice();

    // The containing element for this searchable list
    const elem = document.getElementById(elementId);

    // The search bar
    elem.innerHTML = `<div id="journal-search-bar">
                         <form id="journal-search-form" class="inline-searchbar">

                           <label for="journal-search-text" class="sr-only">Search</label>
                           <input type="text" name="search-text" id="journal-search-text" class="form-control inline-searchbar-item" autocomplete="off">

                           <label for="journal-search-after" class="sr-only">After</label>
                           <input type="date" name="after" id="journal-search-after" class="form-control inline-searchbar-item">
                       
                           <label for="journal-search-before" class="sr-only">Before</label>
                           <input type="date" name="before" id="journal-search-before" class="form-control inline-searchbar-item">
                           
                           <!--
                           <label class="sr-only">Near Me</label>
                           <input type="checkbox" name="near-me" class="form-control">
                           -->
                           
                           <button type="submit" class="transparent-button"><i class="fa fa-search fa-2x"></i></button>
                         </form>
                       </div>`;


    /**
     * Add a new entry to the list
     *
     * @param entry
     */
    this.add = (entry) => {

        // TODO: There is the issue of whether the new entry satisfies the current filter

        this.entries.push(entry);
        this.entries.sort((a, b) => {
            return Math.sign(b.timestamp - a.timestamp);
        });

        this.resultSet.push(entry);
        this.sort();
        this.display();
    };


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
    const _filterText = (rawSearchTerm) => {

        // Ignore case when searching
        const searchTerm = rawSearchTerm.toLowerCase();

        this.resultSet = this.resultSet.filter(function (elem) {
            return elem.text.toLowerCase().includes(searchTerm);
        });

        // Highlight the search term wherever it is found
        this.resultSet.forEach((elem) => {
            elem.text = elem.text.replace(new RegExp(searchTerm, 'i'), "<span class='search-highlight'>$&</span>")
        })
    };

    /**
     * Filter journal entries by date
     *
     * @param from
     * @param to
     * @private
     */
    const _filterDates = (from, to) => {

        this.resultSet = this.resultSet.filter((elem) => {
            return from <= elem.timestamp && elem.timestamp <= to;
        });

    };


    /**
     * Find all entries near the given coordinates
     *
     * @param coords The position to compare against
     * @param radius The radius of inclusion
     */
    const _filterNear = (coords, radius) => {

        this.resultSet = this.resultSet.filter(function (elem) {

            return distance(coords[0], coords[1], elem.pos[0], elem.pos[1]) < radius;
        });

    };


    /**
     * Filter the journal entries according to the filter form
     *
     * @param formData
     */
    this.filter = function (formData) {

        // Start with a clean result set
        this.resultSet = this.entries.map((elem) => {
            return Object.assign({}, elem);
        });

        formData.forEach((field) => {

            if (field['name'] === 'search-text') {

                _filterText(field['value']);
            } else if (field['name'] === 'near-me') {
                _filterNear([0, 0], 100)
            } else if (field['name'] === 'after') {

                const d = (new Date(field['value'])).getTime();

                if (field['value'] !== "") {
                    this.resultSet = this.resultSet.filter((entry) => {
                        return d < entry.timestamp;
                    });
                }

            } else if (field['name'] === 'before') {

                const d = (new Date(field['value'])).getTime();

                if (field['value'] !== "") {
                    this.resultSet = this.resultSet.filter((entry) => {
                        return entry.timestamp < d;
                    });
                }
            }
        })

    };

    // The containing element for the list
    const entryContainer = document.createElement('div');
    elem.append(entryContainer);

    let searchForm = $('#journal-search-form');

    // Set up the handler for the journal search bar
    searchForm.submit((event) => {

        event.preventDefault();

        // TESTING - DEV
        console.log(searchForm.serializeArray());

        this.filter(searchForm.serializeArray());
        this.display();
    });

    // The initial display
    this.sort();
    this.display();

}