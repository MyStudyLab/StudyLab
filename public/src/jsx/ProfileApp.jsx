import React from 'react';
import ReactDom from 'react-dom';
import JournalEntryList from './JournalEntryList.jsx';

import styles from '../css/ProfileApp.css';

class ProfileApp extends React.Component {

    constructor(props) {

        super(props);

        this.state = {
            entries: []
        }
    }

    render() {
        return (

            <div id='UserProfile'>

                <div id='UserProfileInfo'>
                    <span id='UserInfoUsername'>{this.props.profileUsername}</span>
                    <div id='UserInfoAbout'>{this.state.about}</div>
                </div>

                <div id='UserProfileEntries'>
                    <JournalEntryList
                        className="JournalEntryList"
                        items={this.state.entries}
                        publicDisplay={true}
                        filter={""}
                        display={true}
                        handleDelete={() => null}
                        handlePublic={() => null}
                    />
                </div>

            </div>
        )
    }

    /**
     * Load the user's public journal entries once the app has mounted
     */
    componentDidMount() {

        // Get the user's basic profile data
        $.ajax({
            type: "get",
            url: "/json/user/" + this.props.profileUsername,
            dataType: "json"
        }).fail(() => {

            console.log("Error retrieving user info");

        }).done((responseData) => {

            this.setState({
                about: responseData.payload.about
            });

        });

        // Get the user's public journal entries
        $.ajax({
            type: "get",
            url: "/json/journal/public/" + this.props.profileUsername,
            dataType: "json"
        }).then((responseData) => {

            // TESTING - DEV
            console.log(responseData);

            // Sort the items by timestamp, newest first
            const entries = responseData.payload;
            entries.sort((a, b) => Math.sign(b.timestamp - a.timestamp));

            this.setState({
                entries: entries
            });
        });

    }

}

ReactDom.render(
    <ProfileApp profileUsername={profileUsername}/>,
    document.getElementById("ProfileApp")
);