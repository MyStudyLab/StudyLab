import React from 'react';

/**
 * A single item in the journal list
 */
export default class JournalEntry extends React.Component {

    render() {
        return (
            <div id={this.props.item.id} className="JournalEntry partialBorder" onClick={this.props.handleClick}>

                <p className="inferredSubjectList">{this.props.item.inferredSubjects.join(", ").replace(new RegExp("_", "g"), " ")}</p>

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

                <div className="JournalEntryControl">
                    <button
                        onClick={this.props.handlePublic}
                        className={["JournalEntryPublicity",
                            "transparentButton",
                            "entryControlItem",
                            (this.props.item.public ? "active" : "")].join(" ")}
                    >
                        <i className="fa fa-users"/>
                    </button>

                    {
                        // Only display the 'share' button when an entry is public
                        (this.props.item.public) && (
                            <button className="transparentButton entryControlItem">
                                <i className="fa fa-share"/>
                            </button>
                        )
                    }

                    {
                        (this.props.selected) && (
                            <button onClick={this.props.handleDelete}
                                    className="JournalEntryDelete transparentButton entryControlItem">
                                <i className="fa fa-trash"/>
                            </button>
                        )
                    }
                    <div
                        className="journalEntryTimestamp entryControlItem">{moment(this.props.item.timestamp).format('YYYY-MM-DD HH:mm')}
                    </div>
                </div>
                
            </div>
        )
    }
}