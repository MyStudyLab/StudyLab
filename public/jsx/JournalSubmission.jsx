import React from 'react';

class JournalSubmission extends React.Component {

    constructor(props) {

        super(props);

    }

    render() {

        return (
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

                    <div id="journalSubmissionControl">
                        <i id="journalSubmissionGeoButton" className="fa fa-globe fa-lg"/>
                        <button type="submit" id="entrySubmit" className="transparentButton">
                            <i className="fa fa-paper-plane-o fa-lg"/>
                        </button>
                    </div>
                }

            </form>
        )

    }

}