package taneltomson.discord.commands.listeners;


import java.util.Collections;
import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import taneltomson.discord.commands.data.Inspires;

import static taneltomson.discord.data.ChannelIds.GENERAL_TEXT_ID;


@Slf4j
public class InspireListener extends CommandListener {
    @Override
    protected List<Long> getAllowedChannelIds() {
        return Collections.singletonList(GENERAL_TEXT_ID);
    }

    @Override
    protected void handleCommand(String command, String arguments,
                                 MessageReceivedEvent event) {
        if ("inspire".equals(command) && arguments.isEmpty()) {
            final List<String> values = Inspires.Inspires;
            final String message = values.get((new Random()).nextInt(values.size()));

            event.getChannel().sendMessage(message);
        }
    }
}
