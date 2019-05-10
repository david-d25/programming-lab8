package ru.david.room.client;

class Command {
    String name;
    String argument;

    Command(String command) {
        int spaceIndex = command.indexOf(' ');
        if (spaceIndex == -1)
            name = command;
        else {
            name = command.substring(0, spaceIndex);
            argument = command.substring(spaceIndex + 1);
        }
    }
}
