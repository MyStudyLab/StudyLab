import React from 'react';

export default class JournalSubmissionForm extends React.Component {

    constructor(props) {

        super(props);

    }

    render() {

        return (
            <form onSubmit={this.props.handleSubmit}
                  className="AddItemForm"
                  id="journalEntryForm"
                  onFocus={this.props.handleWritingFocus}
                  onBlur={this.props.handleWritingBlur}
            >

                <textarea
                    name="text"
                    className={this.props.writingMode ? "journalEntryActive" : "journalEntryInactive"}
                    id="journalEntryInput"
                    form="journalEntryForm"
                    placeholder="Dear Journal..."
                    autoComplete="off"
                    onChange={this.props.handleChange}
                    value={this.props.text}
                    required
                />

                <div id="journalSubmissionControl">

                    <button onClick={this.props.handleGeoToggle} id="journalSubmissionGeoButton"
                            className={`${this.props.useGeo ? "active" : ""} ${this.props.writingMode ? "" : "vanish"} fa fa-globe fa-lg transparentButton`}
                    />


                    <button type="submit" id="journalSubmissionButton"
                            className={`fa fa-paper-plane-o fa-lg transparentButton ${this.props.writingMode ? "" : "vanish"}`}
                            disabled={this.props.text.length === 0}
                    />
                </div>

            </form>
        )

    }

}