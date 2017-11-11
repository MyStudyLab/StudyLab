import React from 'react';
import JournalEntry from './JournalEntry.jsx';

/**
 * A filterable list of journal entries
 *
 */
export default class JournalEntryList extends React.Component {

    constructor(props) {
        super(props);

        this.state = {selected: ""};

        this.selectHandler = this.selectHandler.bind(this);
    }

    selectHandler(id) {

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
                                    selected={this.state.selected === item._id.$oid}
                                    highlightText={filterText}
                                    handleClick={() => this.selectHandler(item._id.$oid)}
                                    handleDelete={() => this.props.handleDelete(item._id.$oid)}
                                    handlePublic={(e) => this.props.handlePublic(e, item._id.$oid, !item.public)}
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