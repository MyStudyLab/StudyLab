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
 * @param entries The list of journal entries in GeoJson format
 * @param filterCallback [function] A function to call each time the entry list is filtered
 * @constructor
 */
function JournalEntryList(elementId, entries, filterCallback = () => {
}) {

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

    // The earliest timestamp in the list
    const minTimestamp = Math.min(...entries.map((entry) => {
        return entry.properties.timestamp;
    }));

    // The most recent timestamp in the list
    const maxTimestamp = Math.max(...entries.map((entry) => {
        return entry.properties.timestamp;
    }));

    // The search bar
    elem.innerHTML = `<div id="journal-search-bar">
                         <form id="journal-search-form" class="inline-searchbar">

                           <div style="width: 50%; display: inline-flex;">
                             <label for="journal-search-text" class="sr-only">Search</label>
                             <input type="text" name="search-text" id="journal-search-text" class="form-control inline-searchbar-item journal-form-input" autocomplete="off">
                           </div>

                           <div style="width: 50%; display: inline-flex;">
                             <label for="journal-search-after" class="sr-only">After</label>
                             <input type="date" name="after" id="journal-search-after" class="form-control inline-searchbar-item journal-form-input">
                       
                             <label for="journal-search-before" class="sr-only">Before</label>
                             <input type="date" name="before" id="journal-search-before" class="form-control inline-searchbar-item journal-form-input">
                           </div>
                           
                           <div style="width: 100%; display: inline-flex;">
                           
                             <label for="sentiment-slider" class="sr-only">Sentiment Slider</label>
                             <input type="range" name="sentiment-slider" id="sentiment-slider" class="form-control inline-searchbar-item journal-form-input transparent-button" min="0.0" max="1.0" step="0.01">
                         
                         
                             <div class="form-group">
                               <div class="checkbox">
                                 <label for="sentiment-toggle" class="">
                                   <input type="checkbox" name="sentiment-toggle" id="sentiment-toggle" class="inline-searchbar-item journal-form-toggle transparent-button">
                                 </label>
                               </div>
                             </div>
                             
                             <label for="timeline-slider" class="sr-only">Sentiment Slider</label>
                             <input type="range" name="timeline-slider" id="timeline-slider" class="form-control inline-searchbar-item journal-form-input transparent-button" min="0.0" max="1.0" step="0.01">
                         
                        
                             <div class="checkbox form-group">
                               <label for="timeline-toggle" class="">
                                 <input type="checkbox" name="timeline-toggle" id="timeline-toggle" class="form-check-input inline-searchbar-item journal-form-toggle transparent-button">
                               </label>
                             </div>
                             
                           </div>
                           
                         </form>
                       </div>`;


    /**
     * Add a new entry to the list
     *
     * @param entry
     */
    this.add = (entry) => {

        // TODO: There is the issue of whether the new entry satisfies the current filter

        const dc = JSON.parse(JSON.stringify(entry));

        this.entries.unshift(dc);
        this.resultSet.unshift(dc);
    };


    /**
     * Display the entry list in its current state
     */
    this.display = () => {

        // Clear the container before displaying
        entryContainer.innerHTML = "";

        let entryHtml = this.resultSet.map((entry) => {

            return `<div class='journal-entry-text partial-border center-text-content' id="${entry.properties.id}">
                      <p class="inferredSubjectList">${entry.properties.inferredSubjects.join(", ").replace(new RegExp("_", "g"), " ")}</p>
                      <p>${entry.properties.text}</p>
                      <div class="journal-entry-control-bar">
                        <form class="journal-publicity-form control-bar-item">
                          <input type="text" name="id" value="${entry.properties.id}" hidden>
                          <input type="text" name="public" value="${!entry.properties.public}" hidden>
                          <button type="submit" class="transparent-button" style="color: ${entry.properties.public ? "#78A4FF" : "#707073"};"><i class="fa fa-users"></i></button>
                        </form>
                        <form class="journal-delete-form control-bar-item">
                          <input type="text" name="id" value="${entry.properties.id}" hidden>
                          <button type="submit" class="transparent-button journal-delete-button"><i class="fa fa-trash"></i></button>
                        </form>
                        <div class="journal-entry-timestamp control-bar-item">${moment(entry.properties.timestamp).format('YYYY-MM-DD HH:mm')}</div>
                      </div>
                    </div>`;
        });

        entryContainer.innerHTML = entryHtml.join("");

        // Get a reference to the current JournalEntryList object
        let self = this;

        // Handler for deletion
        $('.journal-delete-form').submit(function (e) {

            // Prevent the form from clearing
            e.preventDefault();

            if (confirm("Delete this entry forever?")) {

                // Submit the form asynchronously
                $.ajax({
                    method: "post",
                    url: "/journal/delete",
                    data: $(this).serialize(),
                    dataType: "json",
                    success: (responseData, textStatus, jqXHR) => {
                        if (responseData.payload) {
                            self.remove(responseData.payload);
                        }
                    }
                });
            }

        });

        // Handler for publicity setting
        $('.journal-publicity-form').submit(function (e) {

            // Prevent the form from clearing
            e.preventDefault();

            if (confirm("Change the publicity of this entry?")) {

                // Submit the form asynchronously
                $.ajax({
                    method: "post",
                    url: "/journal/publicity",
                    data: $(this).serialize(),
                    dataType: "json",
                    success: (responseData, textStatus, jqXHR) => {
                        if (responseData['success'] === true) {
                            console.log(responseData);
                            // TODO: change publicity on front-end
                        }
                    }
                });
            }

        });

    };


    /**
     * Remove an entry from the list
     *
     * @param entry_id
     */
    this.remove = function (entry_id) {

        // Remove element from page
        document.getElementById(entry_id).remove();

        // Remove element from entry list
        this.entries = this.entries.filter((entry) => {
            return entry.properties.id !== entry_id;
        });

        // Remove element from result set
        this.resultSet = this.resultSet.filter((entry) => {
            return entry.properties.id !== entry_id;
        });

    };


    /**
     * Sort the entries by timestamp
     *
     * @param oldestFirst
     */
    this.sort = function (oldestFirst = false) {

        const factor = (oldestFirst === true) ? -1 : 1;

        this.resultSet.sort((a, b) => {

            return factor * Math.sign(b.properties.timestamp - a.properties.timestamp);
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
            return elem.properties.text.toLowerCase().includes(searchTerm);
        });

        // Highlight the search term wherever it is found
        this.resultSet.forEach((elem) => {
            elem.properties.text = elem.properties.text.replace(new RegExp(searchTerm, 'i'), "<span class='search-highlight'>$&</span>")
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
            return from <= elem.properties.timestamp && elem.properties.timestamp <= to;
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

            return distance(coords[0], coords[1], elem.geometry.coordinates[0], elem.geometry.coordinates[1]) < radius;
        });
    };


    const passesFilter = (entry, formData) => {

        let passes = true;

        formData.forEach((field) => {

            if (field.name === 'search-text') {

            }


        });

        return passes;

    };

    /**
     * Filter the journal entries according to the filter form
     *
     * TODO: There is a visual bug caused by map circles being redrawn after they are sorted.
     *
     * TODO: Refactor so that the entry list is only iterated over once
     *
     * @param formData
     */
    this.filter = (formData) => {

        // Start with a clean result set - deep copy
        this.resultSet = this.entries.map((entry) => {
            return JSON.parse(JSON.stringify(entry));
        });

        let sentimentToggle = false;
        let sentimentSlider = 0.5;
        const sentimentRadius = 0.2;

        let timelineToggle = false;
        let timelineSlider = 0.5;
        const timelineRadius = 0.2;

        let afterToggle = false;
        let afterDate = 0;

        let beforeToggle = false;
        let beforeDate = (new Date()).getTime();

        // Apply each of the filters specified in the form
        formData.forEach((field) => {

            // Substring search of entry text
            if (field['name'] === 'search-text') {

                _filterText(field['value']);
            }

            // Proximity search
            else if (field['name'] === 'near-me') {
                _filterNear([0, 0], 100)
            }

            // Find entries older than
            else if (field['name'] === 'after') {

                if (field['value'] !== "") {
                    afterToggle = true;
                    afterDate = (new Date(field['value'])).getTime();
                }
            }

            // Find entries younger than
            else if (field['name'] === 'before') {

                if (field['value'] !== "") {
                    beforeToggle = true;
                    beforeDate = (new Date(field['value'])).getTime();
                }
            }

            // Record that the sentiment slider should be used
            else if (field['name'] === 'sentiment-toggle' && field['value'] === 'on') {

                sentimentToggle = true;
            }

            // Get the value of the sentiment slider
            else if (field['name'] === 'sentiment-slider') {
                sentimentSlider = field['value'];
            }

            // Record that the timeline slider should be used
            else if (field['name'] === 'timeline-toggle' && field['value'] === 'on') {

                timelineToggle = true;
            }

            // Get the value of the timeline slider
            else if (field['name'] === 'timeline-slider') {
                timelineSlider = field['value'];
            }
        });

        // Filter the entries by the search criteria
        this.resultSet = this.resultSet.filter((entry) => {

            // Filter by sentiment value
            if (sentimentToggle === true) {

                if (Math.abs(sentimentSlider - entry.properties.sentiment) > sentimentRadius) {
                    return false;
                }
            }

            // Filter by timeline value
            if (timelineToggle === true) {
                if (Math.abs(timelineSlider - ((entry.properties.timestamp - minTimestamp) / (maxTimestamp - minTimestamp))) > timelineRadius) {
                    return false;
                }
            }

            if (afterToggle === true) {
                if (entry.properties.timestamp < afterDate) {
                    return false;
                }
            }

            if (beforeToggle === true) {
                if (entry.properties.timestamp > beforeDate) {
                    return false;
                }
            }

            // The entry didn't fail any filter test. Include it in the result set
            return true;

        });


        // Pass the updated result set to the callback
        filterCallback(this.resultSet);
    };

    // The containing element for the list
    const entryContainer = document.createElement('div');
    elem.append(entryContainer);

    // Handlers for checkboxes
    $('.journal-form-toggle').on('change', () => {

        const formData = $('#journal-search-form').serializeArray();

        this.filter(formData);
        this.sort();
        this.display();
    });

    // Handlers for other inputs
    $('.journal-form-input').on('input', () => {

        const formData = $('#journal-search-form').serializeArray();

        this.filter(formData);
        this.sort();
        this.display();
    });

    $('#journal-search-form').submit((event) => {
        event.preventDefault();
    });

    // The initial display
    this.sort();
    this.display();

}