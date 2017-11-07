import React from 'react';
import JournalEntry from './JournalEntry.jsx';

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

module.exports = JournalEntryList;