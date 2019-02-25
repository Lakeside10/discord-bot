package taneltomson.discord.commands.listeners;


import java.util.Collections;
import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import taneltomson.discord.commands.data.Vegetables;

import static taneltomson.discord.data.ChannelIds.GENERAL_TEXT_ID;


@Slf4j
public class VegetableListener extends CommandListener {
    @Override
    protected List<Long> getAllowedChannelIds() {
        return Collections.singletonList(GENERAL_TEXT_ID);
    }

    @Override
    protected void handleCommand(String command, String arguments, MessageReceivedEvent event) {
        if ("veg".equals(command) && arguments.isEmpty()) {
            final List<String> vegetables = Vegetables.VEGETABLES;
            final String message = vegetables.get((new Random()).nextInt(vegetables.size()));

            log.debug("Sending message: {}", message);
            event.getChannel().sendMessage(message);
        }
    }
}
