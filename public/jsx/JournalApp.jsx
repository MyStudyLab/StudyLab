import React from 'react';
import ReactDOM from 'react-dom';
import JournalSubmissionForm from './JournalSubmissionForm.jsx';
import JournalEntryList from './JournalEntryList.jsx';

import styles from '../stylesheets/JournalApp.css';

/**
 * The top-level journaling app
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

                <JournalSubmissionForm
                    text={this.state.text}
                    writingMode={this.state.writingMode}
                    useGeo={this.state.useGeo}
                    handleChange={this.handleChange}
                    handleSubmit={this.handleSubmit}
                    handleWritingFocus={this.handleWritingFocus}
                    handleWritingBlur={this.handleWritingBlur}
                    handleGeoToggle={this.handleGeoToggle}
                />

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
     * When the user clicks the geo button
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