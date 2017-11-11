import React from 'react';
import ReactDOM from 'react-dom';
import JournalEntryList from './JournalEntryList.jsx';

/**
 *
 */
class JournalApp extends React.Component {

    constructor(props) {

        // Call parent constructor
        super(props);

        // Set the initial state of the app
        this.state = {
            items: [],
            text: '',
            searchText: '',
            writingMode: false,
            useGeo: true
        };

        // Define 'this' in each of the handlers

        this.handleWritingFocus = this.handleWritingFocus.bind(this);
        this.handleWritingBlur = this.handleWritingBlur.bind(this);

        this.handleChange = this.handleChange.bind(this);
        this.handleGeoToggle = this.handleGeoToggle.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);

        this.handleSearch = this.handleSearch.bind(this);

        this.setPublicity = this.setPublicity.bind(this);
        this.deleteItem = this.deleteItem.bind(this);
    }

    render() {
        return (
            <div className="JournalApp">

                <form onSubmit={this.handleSubmit}
                      className="AddItemForm"
                      id="journalEntryForm"
                      onFocus={this.handleWritingFocus}
                      onBlur={this.handleWritingBlur}
                >

                    <textarea
                        name="text"
                        className={this.state.writingMode ? "journalEntryActive" : "journalEntryInactive"}
                        id="journalEntryInput"
                        form="journalEntryForm"
                        placeholder="Dear Journal..."
                        autoComplete="off"
                        onChange={this.handleChange}
                        value={this.state.text}
                        required
                    />

                    {
                        // TODO - Submission doesn't work when including this line
                        //(this.state.writingMode) &&

                        <div id="journalSubmissionControl" className="transparentButton">

                            <button onClick={this.handleGeoToggle} id="journalSubmissionGeoButton"
                                    className={`transparentButton ${this.state.useGeo ? "active" : ""}`}>
                                <i className="fa fa-globe fa-lg"/>
                            </button>


                            <button type="submit" id="journalSubmissionButton" className="transparentButton">
                                <i className="fa fa-paper-plane-o fa-lg"/>
                            </button>
                        </div>
                    }

                </form>


                {
                    (!this.state.writingMode) &&
                    <form onSubmit={e => e.preventDefault()}>
                        <input
                            type="text"
                            placeholder="Search"
                            id="journalSearch"
                            className="SearchFormText"
                            onChange={this.handleSearch}
                            value={this.state.searchText}
                            autoComplete="off"
                        />
                    </form>
                }

                <JournalEntryList
                    className="JournalEntryList"
                    items={this.state.items}
                    filter={this.state.searchText}
                    display={!this.state.writingMode}
                    handleDelete={this.deleteItem}
                    handlePublic={this.setPublicity}
                />

            </div>
        );
    }


    /**
     * Load the current journal entries once the app has mounted
     */
    componentDidMount() {

        $.ajax({
            type: "get",
            url: "/json/journal",
            dataType: "json",
            success: (responseData) => {

                // TESTING - DEV
                console.log(responseData);

                // Sort the items by timestamp, newest first
                let items = responseData.payload;
                items.sort((a, b) => Math.sign(b.timestamp - a.timestamp));

                this.setState({
                    items: items
                })

            }
        });

    }


    /**
     * When the user is writing
     *
     * @param e
     */
    handleWritingFocus(e) {

        this.setState({writingMode: true});

    }

    /**
     * When the user stops writing
     *
     * @param e
     */
    handleWritingBlur(e) {

        this.setState({writingMode: false});
    }

    /**
     * User clicks the geo button
     *
     * @param e
     */
    handleGeoToggle(e) {

        e.preventDefault();

        this.setState(prevState => ({
            useGeo: !prevState.useGeo
        }));

    }

    /**
     *
     * @param e
     */
    handleSearch(e) {

        this.setState({searchText: e.target.value});

    }


    /**
     *
     * @param e
     */
    handleChange(e) {

        this.setState({text: e.target.value});

    }


    /**
     * Set the publicity of a journal entry
     *
     * @param id The id of the journal entry
     * @param publicity The publicity status
     */
    setPublicity(id, publicity) {

        const data = {
            id: id,
            public: publicity
        };

        submitDataAsync(data, "journal/publicity", false, (responseData) => {

            console.log(responseData);

            if (responseData.success === true) {
                this.setState(prevState => ({
                    items: prevState.items.map((item) => {

                        if (item._id.$oid === id) {
                            item.public = publicity;
                        }

                        return item;
                    })
                }));
            }
        });
    }


    /**
     * Delete a journal entry from the list
     *
     * @param id The id of the entry to delete
     */
    deleteItem(id) {

        if (confirm("Delete this entry forever?")) {

            submitDataAsync({id: id}, "/journal/delete", false, (responseData) => {

                console.log(responseData);

                if (responseData.success === true) {
                    this.setState(prevState => ({
                        items: prevState.items.filter((item) => {
                            return item._id.$oid !== id;
                        })
                    }));
                }

            });
        }

    }


    /**
     * Add a journal entry to the list
     *
     * @param e The submission event of the journal entry form
     */
    handleSubmit(e) {

        e.preventDefault();

        if (this.state.text.length === 0) {
            return;
        }

        const newItem = {
            text: this.state.text,
            latitude: 0,
            longitude: 0
        };

        submitDataAsync(newItem, "/journal/add", this.state.useGeo, (responseData) => {

            console.log(responseData);

            // Add the item to the front of the list
            this.setState(prevState => ({
                items: [responseData.payload].concat(prevState.items),
                text: '',
                writingMode: false,
                useGeo: true
            }));
        });
    }
}

// Render the app
ReactDOM.render(
    <JournalApp/>,
    document.getElementById("JournalApp")
);