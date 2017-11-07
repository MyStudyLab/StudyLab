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
        this.handleSearchSubmit = this.handleSearchSubmit.bind(this);
        this.setPublicity = this.setPublicity.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.deleteItem = this.deleteItem.bind(this);
    }

    render() {
        return (
            <div className="JournalApp">

                <form onSubmit={this.handleSubmit} className="AddItemForm">

                    <textarea
                        name="text"
                        className="form-control"
                        id="journalEntryInput"
                        form="journal-entry-form"
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

                <form onSubmit={this.handleSearchSubmit}>
                    <input
                        type="text"
                        placeholder="Search"
                        id="journalSearch"
                        className="SearchFormText"
                        onChange={this.handleSearch}
                        value={this.state.searchText}
                    />
                </form>

                <JournalEntryList
                    className="TodoItemList"
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


    handleSearch(e) {

        this.setState({searchText: e.target.value});

    }


    handleSearchSubmit(e) {

        e.preventDefault();

    }


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

// A single item in the journal list
class JournalEntry extends React.Component {

    render() {
        return (
            <div id={this.props.item.id} className="TodoItem partialBorder centerTextContent">

                <p className="TodoItemText">
                    {
                        // Highlight the specified text in each item
                        this.props.item.text
                            .split(new RegExp(`(${this.props.highlightText})`, "i"))
                            .map((text, i) => {
                                if ((i % 2) === 0) {
                                    return text;
                                } else {
                                    return <span className="textHighlight">{text}</span>
                                }
                            })
                    }
                </p>

                <div className="TodoItemControl">
                    <button
                        onClick={this.props.handlePublic}
                        className={["TodoItemPublicity",
                            "transparentButton",
                            "entryControlItem",
                            (this.props.item.public ? "active" : "")].join(" ")}
                    >
                        <i className="fa fa-users"/>
                    </button>

                    <button onClick={this.props.handleDelete}
                            className="TodoItemDelete transparentButton entryControlItem">
                        <i className="fa fa-trash"/>
                    </button>
                    <div
                        className="journalEntryTimestamp entryControlItem">{moment(this.props.item.timestamp).format('YYYY-MM-DD HH:mm')}
                    </div>
                </div>
            </div>
        )
    }
}

class JournalEntryList extends React.Component {
    render() {

        let filterText = this.props.filter.toLowerCase();

        return (
            <div>
                {
                    this.props.items
                        .filter(item => item.text.toLowerCase().includes(filterText))
                        .map(item => (
                            <JournalEntry
                                key={item._id.$oid}
                                item={item}
                                highlightText={filterText}
                                handleDelete={() => this.props.handleDelete(item._id.$oid)}
                                handlePublic={() => this.props.handlePublic(item._id.$oid, !item.public)}
                            />
                        ))}
            </div>
        );
    }
}

// Render the app
ReactDOM.render(
    <JournalApp/>,
    document.getElementById("JournalApp")
);