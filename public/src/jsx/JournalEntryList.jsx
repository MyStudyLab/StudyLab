import React from 'react';
import JournalEntry from './JournalEntry.jsx';

// Stylesheet
import styles from '../css/JournalEntryList.css'

/**
 * A filterable list of journal entries
 *
 */
export default class JournalEntryList extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            selected: "" // The _id of the selected item
        };

        this.selectHandler = this.selectHandler.bind(this);
    }

    /**
     * Sets the currently selected entry in the list
     *
     * @param id The id of the element to select
     */
    selectHandler(id) {

        if (window.getSelection().toString().length !== 0) {
            return;
        }


        this.setState(prevState => (
            {selected: (prevState.selected === id ? "" : id)}
        ));
    }

    render() {

        let filterText = this.props.filter.toLowerCase();

        if (this.props.display) {
            return (
                <div className="">
                    {
                        this.props.items
                            .filter(item => item.text.toLowerCase().includes(filterText))
                            .map(item => (
                                <JournalEntry
                                    key={item._id.$oid}
                                    item={item}
                                    publicDisplay={this.props.publicDisplay}
                                    selected={this.state.selected === item._id.$oid}
                                    highlightText={filterText}
                                    handleClick={() => this.selectHandler(item._id.$oid)}
                                    handleDelete={(e) => {

                                        e.stopPropagation();

                                        this.props.handleDelete(item._id.$oid)
                                    }}
                                    handlePublic={(e) => {

                                        e.stopPropagation();

                                        this.props.handlePublic(item._id.$oid, !item.public)
                                    }}
                                />
                            ))}
                </div>
            );
        } else {
            // In this case, do not render
            return null;
        }


    }
}