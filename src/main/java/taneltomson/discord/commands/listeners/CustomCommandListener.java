package taneltomson.discord.commands.listeners;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import taneltomson.discord.common.model.Command;
import taneltomson.discord.common.service.CustomCommandService;

import static taneltomson.discord.data.ChannelIds.BOT_TEST_TEXT_ID;
import static taneltomson.discord.data.ChannelIds.GENERAL_TEXT_ID;


@Slf4j
public class CustomCommandListener extends CommandListener {
    public static final String CREATION_ERROR_RESPONSE = "Command already exists.";
    public static final String CREATION_MENTION_ERROR_RESPONSE = "No mentions you ass!";
    public static final String CREATION_SUCCESS_RESPONSE = "Command created!";
    public static final String DELETION_ERROR_RESPONSE = "No such command.";
    public static final String DELETION_SUCCESS_RESPONSE = "Command Deleted.";
    private static final List<String> RESERVED_COMMANDS = Arrays.asList(
            "create", "delete", "inspire", "veg", "addsqb", "removesqb", "rank", "top", "joined",
            "points", "help", "watch", "last", "watchpoints");

    private final CustomCommandService service;

    public CustomCommandListener(CustomCommandService service) {
        this.service = service;
    }

    @Override
    protected List<Long> getAllowedChannelIds() {
        return Arrays.asList(GENERAL_TEXT_ID, BOT_TEST_TEXT_ID);
    }

    @Override
    protected void handleCommand(String command, String arguments, MessageReceivedEvent event) {
        log.info("Command: {}, arguments: {}, Command in channel: {}",
                 command, arguments, event.getChannel().getName());
        if (BOT_TEST_TEXT_ID.equals(event.getChannel().getLongID())) {
            if ("create".equals(command) && !arguments.isEmpty()) {
                if (!event.getMessage().getMentions().isEmpty() ||
                        !event.getMessage().getRoleMentions().isEmpty()) {
                    log.info("User {} prevented from creating mention command",
                             event.getMessage().getAuthor().getDisplayName(event.getGuild()));
                    event.getChannel().sendMessage(CREATION_MENTION_ERROR_RESPONSE);
                } else {
                    log.info("Received create command request, arguments: {}", arguments);
                    handleCreateCommand(arguments, event);
                }
            } else if ("delete".equals(command) && !arguments.isEmpty()
                    && !arguments.contains(" ")) {
                log.info("Received delete command request. arguments: {}", arguments);
                handleDeleteCommand(arguments, event);
                return;
            }

            log.info("correct room but incorrect call");
        }

        if (!RESERVED_COMMANDS.contains(command)) {
            log.info("Received command, command: {}", command);
            handleShowCommand(command, event);
        }
    }

    private void handleShowCommand(String name, MessageReceivedEvent event) {
        if (service.hasCommand(name)) {
            event.getChannel().sendMessage(service.findCommand(name).getValue());
        }
    }

    private void handleDeleteCommand(String commandName, MessageReceivedEvent event) {
        if (!service.hasCommand(commandName)) {
            log.info("Asked to delete command '{}' but doesn't exist", commandName);
            event.getChannel().sendMessage(DELETION_ERROR_RESPONSE);
        } else {
            service.deleteCommand(commandName);
            log.debug("Command '{}' deleted", commandName);
            event.getChannel().sendMessage(DELETION_SUCCESS_RESPONSE);
        }
    }

    private void handleCreateCommand(String arguments, MessageReceivedEvent event) {
        final List<String> argsSplit = new ArrayList<>(Arrays.asList(arguments.split(" ")));

        final String commandName = argsSplit.remove(0).toLowerCase();
        final String commandValue = String.join(" ", argsSplit);
        log.info("commandName: {}, commandValue: {}", commandName, commandValue);

        if (RESERVED_COMMANDS.contains(commandName)) {
            log.debug("handleCreateCommand - command '{}' is reserved", commandName);
            return;
        }

        if (commandName.isEmpty() || commandValue.isEmpty()) {
            log.debug("handleCreateCommand - invalid command creation syntax");
            return;
        }

        if (!service.hasCommand(commandName)) {
            log.info("Creating custom command '{}' with value '{}'", commandName, commandValue);
            service.addCommand(new Command().setCallKey(commandName)
                                            .setValue(commandValue)
                                            .setCreated(LocalDate.now()));
            event.getChannel().sendMessage(CREATION_SUCCESS_RESPONSE);
        } else {
            log.debug("Command '{}' already exists", commandName);
            event.getChannel().sendMessage(CREATION_ERROR_RESPONSE);
        }
    }
}
