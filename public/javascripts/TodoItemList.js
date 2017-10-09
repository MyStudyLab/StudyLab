function TodoItemList(elementId, items) {


    // The element Id containing the list
    this.elementId = elementId;

    // The list of journal entries
    this.items = items;

    // Display the entry list in its current state
    this.display = function () {

        let entryHtml = this.items.map((item) => {

            let text = '<div class="todo-item-text partial-border center-text-content">' + '<p>' + item.text + '</p>';

            let date = '<div class="todo-item-info">' + moment(item.startTime).format('YYYY-MM-DD HH:mm') + '</div>' + '</div>';

            return text + date;
        });

        document.getElementById(this.elementId).innerHTML = entryHtml.join("");
    };

    //
    this.sort = function (oldestFirst = false) {

        const factor = (oldestFirst === true) ? -1 : 1;

        this.items.sort((a, b) => {

            return factor * Math.sign(b.startTime - a.startTime);
        });
    }

}