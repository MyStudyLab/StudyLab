// A single item in the journal list
class JournalEntry extends React.Component {

    render() {
        return (
            <div id={this.props.item.id} className="TodoItem">

                <p className="TodoItemText">
                    {
                        // Highlight the specified text in each item
                        this.props.item.text
                            .split(new RegExp(`(${this.props.highlightText})`, "i"))
                            .map((text, i) => {
                                if ((i % 2) === 0) {
                                    return text;
                                } else {
                                    return <span className="Red">{text}</span>
                                }
                            })
                    }
                </p>

                <div className="TodoItemControl">
                    <button
                        onClick={this.props.handlePublic}
                        className={["TodoItemPublicity",
                            "transparent-button",
                            (this.props.item.public ? "Red" : "Blue"),
                            "TodoItemButton"].join(" ")}
                    >
                        {this.props.item.public ? "public" : "private"}
                    </button>

                    <button onClick={this.props.handleDelete}
                            className="TodoItemDelete"
                    >
                        Delete
                    </button>
                </div>
            </div>
        )
    }
}