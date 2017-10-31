'use strict';


function TodoItemList(elementId, items) {

    // The element Id containing the list
    this.elementId = elementId;

    // The list of journal entries
    this.items = items;

    // Mapping from item IDs to HTML elements
    this.idToElem = {};

    // Display the entry list in its current state
    this.display = function () {

        let container = document.getElementById(elementId);

        // Create and append each item to the list
        this.items.forEach((item) => {

            let d = document.createElement('div');

            // The containing div for the item
            d.classList.add("todo-item-text", "partial-border", "center-text-content");
            d.id = `todo-item-${item["_id"]["$oid"]}`;

            // The text content
            let text = document.createElement('p');
            text.innerHTML = item.text;

            // The timestamp
            let date = document.createElement('div');
            date.classList.add("todo-item-info");
            date.textContent = moment(item.startTime).format('YYYY-MM-DD HH:mm');

            // The completion button
            let f1 = document.createElement('form');
            f1.classList.add("todo-completion-form");
            f1.innerHTML = `<input type="text" name="id" value="${item["_id"]["$oid"]}" hidden><button type="submit" class="todo-completion-button"><i class="fa fa-bullseye fa-lg"></i></button>`;

            // The deletion button
            let f2 = document.createElement('form');
            f2.classList.add("todo-deletion-form");
            f2.innerHTML = `<input type="text" name="id" value="${item["_id"]["$oid"]}" hidden><button type="submit" class="todo-deletion-button"><i class="fa fa-trash fa-lg"></i></button>`;


            // Assemble the item's HTML element
            d.append(text);
            d.append(f1);
            d.append(date);
            d.append(f2);

            // Handler for the completion form
            submitWithGeo(f1, "todo/completeTodoItem", function (responseData) {
                if (responseData['success'] === true) {
                    // TODO: Do stuff with the item's object
                    // Add completion date
                    // Move to the 'completed' portion of the list
                } else {
                    console.log("There was a problem deleting the item")
                }
            });

            // Handler for the deletion form
            submitInBackground(f2, "todo/deleteTodoItem", function (responseData) {
                if (responseData['success'] === true) {
                    d.remove();
                } else {
                    console.log("There was a problem deleting the item")
                }
            });

            // Add the item to the page
            container.append(d);
        });
    };

    // Sort the items by their startTime
    this.sort = function (oldestFirst = false) {

        const factor = (oldestFirst === true) ? -1 : 1;

        this.items.sort((a, b) => {

            return factor * Math.sign(b.startTime - a.startTime);
        });
    };

    // Delete an item from the list
    this.deleteItem = function (itemId) {

        $.ajax({
            method: "post",
            url: "/todo/deleteTodoItem",
            data: {
                id: itemId
            },
            dataType: "json",
            success: function (responseData, textStatus, jqXHR) {

                console.log(responseData);

                // Clear the text input
                if (responseData['success'] === true) {
                    document.querySelector("#todo-item-" + itemId).remove();
                } else {
                    console.log("Error while deleting todo item");
                }
            }
        });
    };

    // Add an item to the list
    this.addItem = function (text) {

        $.ajax({
            method: "post",
            url: "/todo/addTodoItem",
            data: {
                text: text
            },
            dataType: "json",
            success: function (responseData, textStatus, jqXHR) {

                console.log(responseData);

                // Clear the text input
                if (responseData['success'] === true) {
                    document.querySelector("#todo-item-list");
                } else {
                    console.log("Error while deleting todo item");
                }
            }
        });

    };

}