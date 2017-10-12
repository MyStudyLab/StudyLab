'use strict';


function TodoItemList(elementId, items) {


    // The element Id containing the list
    this.elementId = elementId;

    // The list of journal entries
    this.items = items;

    // Display the entry list in its current state
    this.display = function () {

        let entryHtml = this.items.map((item) => {

            let text = '<div class="todo-item-text partial-border center-text-content">' + '<p>' + item.text + '</p>';

            let date = '<div class="todo-item-info">' + moment(item.startTime).format('YYYY-MM-DD HH:mm') + '</div>';

            let f = '<form class="todo-completion-form"><input type="text" name="id" value="' + item["_id"]["$oid"] + '" hidden><button type="submit" class="todo-completion-button"><i class="fa fa-bullseye fa-lg"></i></button></form>';

            let del = '<form class="todo-deletion-form"><input type="text" name="id" value="' + item["_id"]["$oid"] + '" hidden><button type="submit" class="todo-deletion-button"><i class="fa fa-trash fa-lg"></i></button></form>';

            return text + f + date + del + '</div>';
        });

        document.getElementById(this.elementId).innerHTML = entryHtml.join("");

        // Set up handlers for form submission
        submitWithGeo(".todo-completion-form", "/todo/completeTodoItem");

        submitInBackground(".todo-deletion-form", "/todo/deleteTodoItem", false);
    };

    // Sort the items by their startTime
    this.sort = function (oldestFirst = false) {

        const factor = (oldestFirst === true) ? -1 : 1;

        this.items.sort((a, b) => {

            return factor * Math.sign(b.startTime - a.startTime);
        });
    }

}