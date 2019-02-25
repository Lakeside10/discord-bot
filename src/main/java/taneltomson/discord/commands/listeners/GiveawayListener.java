package taneltomson.discord.commands.listeners;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import taneltomson.discord.data.ChannelIds;


@Slf4j
public class GiveawayListener extends CommandListener {
    private final Random random;
    private List<String> participants = new ArrayList<>();
    private List<String> ALLOWED_TO_USE_GIVEAWAY_ROLES = Arrays.asList("Commander", "Officer");

    public GiveawayListener(Random random) {
        super();

        this.random = random;
    }

    @Override
    protected List<Long> getAllowedChannelIds() {
        return Arrays.asList(ChannelIds.GENERAL_TEXT_ID, ChannelIds.BOT_TEST_TEXT_ID);
    }

    @Override
    protected void handleCommand(String command, String arguments, MessageReceivedEvent event) {
        if (!"giveaway".equals(command.toLowerCase()) || arguments == null || arguments.isEmpty()) {
            log.debug("Incorrect invocation of command: command: {}, arguments: {}, channel: {}",
                      command, arguments, event.getChannel().getName());
            return;
        }

        log.debug("command: {}, arguments: {}, channel: {}",
                  command, arguments, event.getChannel().getName());

        if (arguments.toLowerCase().startsWith("add list") && isCalledInBotTestChannel(event)) {
            log.debug("Doing add list");
            List<String> asList = new ArrayList<>(Arrays.asList(arguments.split("\n")));
            asList.remove(0);

            if (!asList.isEmpty()) {
                asList = asList.stream().map(this::discordEscape).collect(Collectors.toList());
                participants.addAll(asList);
                Collections.sort(participants);
                event.getChannel()
                     .sendMessage(String.format("Added %s participants.", asList.size()));
            }
        } else if (arguments.toLowerCase().startsWith("add ") && isCalledInBotTestChannel(event)) {
            log.debug("Doing add");
            String personToAdd = removeFirstWord(arguments);

            if (!personToAdd.isEmpty()) {
                personToAdd = discordEscape(personToAdd);
                participants.add(personToAdd);
                Collections.sort(participants);
                event.getChannel().sendMessage(String.format("Added participant %s.", personToAdd));
            }
        } else if (arguments.toLowerCase().startsWith("delete ")
                && isCalledInBotTestChannel(event)) {
            log.debug("Doing delete");
            final String personToDelete = removeFirstWord(arguments);

            if (!personToDelete.isEmpty()) {
                if (participants.contains(personToDelete)) {
                    participants.remove(personToDelete);
                    Collections.sort(participants);
                    event.getChannel().sendMessage(String.format("Deleted person %s.",
                                                                 personToDelete));
                } else {
                    event.getChannel().sendMessage(String.format("No person %s in giveaway!",
                                                                 personToDelete));
                }
            }
        } else if (arguments.toLowerCase().startsWith("info") && hasPermission(event)) {
            log.debug("Doing info");
            if (participants.isEmpty()) {
                event.getChannel().sendMessage("No giveaway currently set up.");
            } else {
                final StringBuilder sb = new StringBuilder();

                sb.append("Giveaway set up with following participants:\n");
                participants.forEach(name -> sb.append(name).append("\n"));
                sb.append("Each person has equal chance of winning.");

                event.getChannel().sendMessage(sb.toString());
            }
        } else if (arguments.toLowerCase().startsWith("draw") && hasPermission(event)) {
            log.debug("Doing draw");
            if (participants.isEmpty()) {
                event.getChannel().sendMessage("Add participants first!");
            } else {
                final String winner = participants.get(random.nextInt(participants.size()));
                event.getChannel().sendMessage(String.format("The winner is:%n%s", winner));

                final String message = event.getAuthor()
                        + ", please don't forget to clear the participants from the giveaway when "
                        + "you're finished. I'm dumb and don't otherwise know to do it myself. "
                        + "That "
                        + "way the next giveaway can start from a clean sheet."
                        + "\n\n"
                        + "You can do this with the command:"
                        + "\n"
                        + "!giveaway clear";
                event.getClient().getChannelByID(ChannelIds.BOT_TEST_TEXT_ID).sendMessage(message);
            }
        } else if (arguments.toLowerCase().startsWith("clear") && isCalledInBotTestChannel(event)) {
            log.debug("Doing clear");
            participants = new ArrayList<>();
            event.getChannel().sendMessage("Participants cleared, ready to start a new giveaway.");
        } else {
            log.debug("No action");
        }
    }

    private String discordEscape(String string) {
        string = string.replace("*", "\\*");
        string = string.replace("_", "\\_");
        string = string.replace("~", "\\~");

        return string;
    }

    private String removeFirstWord(String arguments) {
        final List<String> asList = new ArrayList<>(Arrays.asList(arguments.split(" ")));
        asList.remove(0);
        return String.join(" ", asList);
    }

    private boolean isCalledInBotTestChannel(MessageReceivedEvent event) {
        return ChannelIds.BOT_TEST_TEXT_ID.equals(event.getChannel()
                                                       .getLongID());
    }

    private boolean hasPermission(MessageReceivedEvent event) {
        event.getAuthor().getRolesForGuild(event.getGuild())
             .forEach(role -> log.debug("hasPermission - author role: {}", role.getName()));
        final boolean hasPermission = event
                .getAuthor().getRolesForGuild(event.getGuild()).stream()
                .anyMatch(role -> ALLOWED_TO_USE_GIVEAWAY_ROLES.contains(role.getName()));
        log.debug("hasPermission: {}", hasPermission);
        return hasPermission;
    }
}
