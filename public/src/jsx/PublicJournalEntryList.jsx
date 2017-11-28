import React from 'react';

import styles from '../css/PublicJournalEntryList.css';

export default class PublicJournalEntryList extends React.Component {

    constructor(props) {

        super(props);

    }

    render() {

        return (

            <div>
                {
                    this.props.entries.map(entry => <div key={entry._id.$oid}>{entry.text}</div>)
                }
            </div>

        )

    }

}