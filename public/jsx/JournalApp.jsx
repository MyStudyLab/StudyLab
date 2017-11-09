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
        this.state = {items: [], text: '', searchText: ''};

        // Define 'this' in each of the handlers
        this.handleChange = this.handleChange.bind(this);
        this.handleSearch = this.handleSearch.bind(this);
        this.setPublicity = this.setPublicity.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.deleteItem = this.deleteItem.bind(this);
    }

    render() {
        return (
            <div className="JournalApp">

                <form onSubmit={this.handleSubmit} className="AddItemForm" id="journalEntryForm">

                    <textarea
                        name="text"
                        className=""
                        id="journalEntryInput"
                        form="journalEntryForm"
                        placeholder="Dear Journal..."
                        autoComplete="off"
                        onChange={this.handleChange}
                        value={this.state.text}
                        required
                    />

                    <button type="submit" id="entrySubmit" className="transparentButton">
                        <i className="fa fa-paper-plane-o fa-lg"/>
                    </button>
                </form>

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

                <JournalEntryList
                    className="JournalEntryList"
                    items={this.state.items}
                    filter={this.state.searchText}
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

        submitDataInBackground(data, "journal/publicity", (responseData) => {

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

            submitDataInBackground({id: id}, "/journal/delete", (responseData) => {

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

        submitDataWithGeo(newItem, "/journal/add", (responseData) => {

            console.log(responseData);

            // Add the item to the front of the list
            this.setState(prevState => ({
                items: [responseData.payload].concat(prevState.items),
                text: '',
                public: false
            }));
        });
    }
}

// Render the app
ReactDOM.render(
    <JournalApp/>,
    document.getElementById("JournalApp")
);