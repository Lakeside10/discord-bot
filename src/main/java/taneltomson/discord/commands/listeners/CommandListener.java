package taneltomson.discord.commands.listeners;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;


@Slf4j
public abstract class CommandListener {
    protected abstract List<Long> getAllowedChannelIds();

    protected abstract void handleCommand(String command, String arguments,
                                          MessageReceivedEvent event);

    @EventSubscriber
    public void onMessageReceivedEvent(final MessageReceivedEvent event) {
        log.info("Received onMessageReceivedEvent, room ID: {}", event.getChannel().getLongID());

        if (!getAllowedChannelIds().contains(event.getChannel().getLongID())) {
            log.info("Incorrect channel - {} - no need to handle", event.getChannel().getName());
            return;
        }

        final String messageContent = event.getMessage().getContent();
        log.info("Message content: '{}'", messageContent);

        if (!messageContent.startsWith("!")) {
            log.info("Message is not a command, no need to handle.");
            return;
        }

        final List<String> messageSplit = new ArrayList<>(Arrays.asList(messageContent
                                                                                .substring(1)
                                                                                .split(" ")));
        final String commandName = messageSplit.get(0);
        log.info("Command name: {}", commandName);

        if (commandName.isEmpty()) {
            log.info("Empty command, no need to handle");
            return;
        }

        messageSplit.remove(0);
        final String commandArguments = String.join(" ", messageSplit);
        log.info("Command arguments: {}", commandArguments);

        handleCommand(commandName, commandArguments, event);
    }
}
