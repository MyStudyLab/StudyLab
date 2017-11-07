/**
 *
 */
class CheckBox extends React.Component {

    constructor(props) {

        // Call parent constructor
        super(props);
    }

    render() {

        return (
            <input
                type='checkbox'
                name="public"
                value="public"
                onChange={this.props.handleToggle}
                checked={this.props.checked}
            />
        )
    }

}