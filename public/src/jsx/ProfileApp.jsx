import React from 'react';
import ReactDom from 'react-dom';

class ProfileApp extends React.Component {

    constructor(props) {
        super(props)
    }

    render() {
        return (

            <div></div>
        )
    }

    /**
     * Load the current journal entries once the app has mounted
     */
    componentDidMount() {

        $.ajax({
            type: "get",
            url: "/json/user/" + this.props.profileUsername,
            dataType: "json",
            success: (responseData) => {

                // TESTING - DEV
                console.log(responseData);

                this.setState({
                    about: responseData.payload.about,
                    updatedAbout: responseData.payload.about
                })

            }
        });

    }

}