package taneltomson.discord.commands.listeners;


import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import taneltomson.discord.util.web.DataNotFoundException;
import taneltomson.discord.util.web.WTWebsiteScraper;
import taneltomson.discord.util.web.data.MemberInfo;
import taneltomson.discord.util.web.data.SquadronInfo;

import static taneltomson.discord.data.ChannelIds.BOT_TEST_TEXT_ID;
import static taneltomson.discord.data.ChannelIds.GENERAL_TEXT_ID;


@Slf4j
public class SquibsCommandListener extends CommandListener {
    private static final String TRY_HARD_COALITION = "xTHCx";

    private final WTWebsiteScraper scraper = new WTWebsiteScraper();

    @Override
    protected List<Long> getAllowedChannelIds() {
        return Arrays.asList(GENERAL_TEXT_ID, BOT_TEST_TEXT_ID);
    }

    @Override
    protected void handleCommand(String command, String argument, MessageReceivedEvent event) {
        if ("points".equals(command) && !argument.isEmpty()) {
            getPlayerSquibsPoints(argument, event);
        } else if ("joined".equals(command) && !argument.isEmpty()) {
            getPlayerJoinDate(argument, event);
        } else if ("top".equals(command)) {
            getTopPlayers(event);
        } else if ("rank".equals(command)) {
            getSquibRank(event);
        }
    }

    private void getSquibRank(MessageReceivedEvent event) {
        log.debug("Squadron rank asked");

        try {
            final SquadronInfo squadronInfo = scraper.scrapeSquadronInfo(TRY_HARD_COALITION);
            event.getChannel()
                 .sendMessage("Rank: " + squadronInfo.getPosition() + " ("
                                      + squadronInfo.getSquibPoints() + " points)");
        } catch (IOException e) {
            log.warn("Unable to get squadron rank.", e);
            sendGenericErrorMessage(event);
        } catch (DataNotFoundException e) {
            event.getChannel().sendMessage("Didn't find our rating. Only looked through first 200"
                                                   + " squadrons. Perhaps we need to try harder!");
        }
    }

    private void getTopPlayers(MessageReceivedEvent event) {
        log.debug("Top players asked");

        try {
            final List<MemberInfo> memberInfos =
                    scraper.getSquadronMembersInfo(WTWebsiteScraper.THC);
            final List<MemberInfo> topPlayers = memberInfos
                    .stream().sorted(Comparator.comparing(MemberInfo::getSquibsPoints).reversed())
                    .limit(25L).collect(Collectors.toList());
            final StringBuilder response = new StringBuilder();

            response.append("Top squibs points:");
            for (MemberInfo player : topPlayers) {
                final int position = topPlayers.indexOf(player) + 1;

                response.append("\n");
                response.append(position);
                response.append(". ");
                response.append(position <= 3 ? "**" : "");
                response.append(player.getDiscordEscapedName());
                response.append(position <= 3 ? "**" : "");
                response.append(" (");
                response.append(player.getSquibsPoints());
                response.append(")");
            }

            log.debug("Sending response: {}", response.toString());
            event.getChannel().sendMessage(response.toString());
        } catch (IOException e) {
            log.warn("Unable to get top squibs players.", e);
            sendGenericErrorMessage(event);
        }
    }

    private void getPlayerJoinDate(String playerName, MessageReceivedEvent event) {
        log.debug("Joined date asked for {}", playerName);

        try {
            final Optional<MemberInfo> memberInfo = getPlayerWithName(playerName);

            if (memberInfo.isPresent()) {
                final MemberInfo member = memberInfo.get();
                final String joinDate = member.getJoinDate().toString();
                event.getChannel().sendMessage(member.getDiscordEscapedName()
                                                       + " joined us on: " + joinDate);
            } else {
                event.getChannel().sendMessage("Player '" + playerName + "' not found.");
            }
        } catch (IOException e) {
            log.warn("Unable to get join date for {}.", playerName, e);
            sendGenericErrorMessage(event);
        }
    }

    private void getPlayerSquibsPoints(String playerName, MessageReceivedEvent event) {
        log.debug("Squibs points asked for {}", playerName);

        try {
            final List<MemberInfo> members = scraper.getSquadronMembersInfo(WTWebsiteScraper.THC);
            final Optional<MemberInfo> memberInfo = getPlayerWithName(playerName);
            final List<MemberInfo> topPlayers = members
                    .stream().sorted(Comparator.comparing(MemberInfo::getSquibsPoints).reversed())
                    .collect(Collectors.toList());

            if (memberInfo.isPresent()) {
                final MemberInfo member = memberInfo.get();
                final int memberRank = topPlayers.indexOf(member) + 1;
                event.getChannel().sendMessage("Points: " + member.getSquibsPoints()
                                                       + " (#" + memberRank + ")");
            } else {
                event.getChannel()
                     .sendMessage("Player '" + playerName + "' not found.");
            }
        } catch (IOException e) {
            log.warn("Unable to get squibs points for {}.", playerName, e);
            sendGenericErrorMessage(event);
        }
    }

    private Optional<MemberInfo> getPlayerWithName(String playerName) throws IOException {
        return scraper.getSquadronMembersInfo(WTWebsiteScraper.THC).stream()
                      .filter(m -> playerName.toLowerCase().equals(m.getName().toLowerCase()))
                      .findFirst();
    }

    private void sendGenericErrorMessage(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
                "There was a problem getting data from WT website. Try again soon(TM) or "
                        + "report to Teo if the problem persists.");
    }
}
