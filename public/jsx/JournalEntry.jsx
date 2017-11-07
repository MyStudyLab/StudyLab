import React from 'react';

/**
 * A single item in the journal list
 */
class JournalEntry extends React.Component {

    render() {
        return (
            <div id={this.props.item.id} className="JournalEntry partialBorder centerTextContent">

                <p className="JournalEntryText">
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

module.exports = JournalEntry;