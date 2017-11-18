import React from 'react';

// Stylesheet
import styles from '../css/JournalEntry.css'

/**
 * A single item in the journal list
 */
export default class JournalEntry extends React.Component {

    constructor(props) {
        super(props);

        this.handleShare = this.handleShare.bind(this);
    }

    handleShare(e) {

        e.stopPropagation();

    }

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

                    {
                        // Only display the 'share' button when an entry is public
                        (this.props.item.public) && (
                            <button className="transparentButton entryControlItem" onClick={this.handleShare}>
                                <i className="fa fa-share"/>
                            </button>
                        )
                    }

                    <div
                        className="journalEntryTimestamp entryControlItem">{moment(this.props.item.timestamp).format('YYYY-MM-DD HH:mm')}
                    </div>
                </div>

                {
                    (this.props.selected) && (
                        <div className="entrySubControl">

                            <button
                                onClick={this.props.handlePublic}
                                className={["JournalEntryPublicity",
                                    "transparentButton",
                                    "entryControlItem",
                                    (this.props.item.public ? "active" : "")].join(" ")}
                            >
                                <i className="fa fa-users"/>
                            </button>


                            <button onClick={this.props.handleDelete}
                                    className="JournalEntryDelete transparentButton entryControlItem">
                                <i className="fa fa-trash"/>
                            </button>

                        </div>
                    )
                }

            </div>
        )
    }
}