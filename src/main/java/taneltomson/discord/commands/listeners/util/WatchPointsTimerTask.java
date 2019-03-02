package taneltomson.discord.commands.listeners.util;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TimerTask;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.IDiscordClient;
import taneltomson.discord.util.MessageHelper;
import taneltomson.discord.util.web.WTWebsiteScraper;
import taneltomson.discord.util.web.data.MemberInfo;

import static taneltomson.discord.data.ChannelIds.BOT_TEST_TEXT_ID;


@Slf4j
public class WatchPointsTimerTask extends TimerTask {
    private static int FAILURES_IN_A_ROW = 0;
    private static boolean PREVIOUS_FAILED = false;
    private final WTWebsiteScraper scraper = new WTWebsiteScraper();
    private final IDiscordClient client;
    @Getter private Integer sessionWins = 0;
    @Getter private Integer sessionLosses = 0;
    private List<MemberInfo> lastMemberInfos = null;
    private Double lastSquadronPoints = null;

    public WatchPointsTimerTask(IDiscordClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        if (FAILURES_IN_A_ROW >= 10) {
            log.info("Cancelling timer task and sending an error message.");
            this.cancel();
            sendErrorMessage();
        }

        try {
            if (PREVIOUS_FAILED) {
                log.info("Sending warning message because previous ran failed.");
                sendWarningMessage();
            }

            log.debug("Timer run - watching points");

            final List<MemberInfo> newMemberInfos =
                    scraper.getSquadronMembersInfo(WTWebsiteScraper.THC);
            watchPoints(newMemberInfos);

            if (FAILURES_IN_A_ROW != 0) {
                FAILURES_IN_A_ROW = 0;
                PREVIOUS_FAILED = false;
                log.info("Timer recovered from errors.");
                sendRecoveredMessage();
            }
            log.debug("Timer run - finished");
        } catch (Exception e) {
            log.error("Timer task ran into a problem.", e);
            FAILURES_IN_A_ROW += 1;
            PREVIOUS_FAILED = true;
            log.error("Failed in a row: {}", FAILURES_IN_A_ROW);
        }
    }

    private double calculateSquadronPoints(List<MemberInfo> members) {
        final List<MemberInfo> sortedByPoints = members
                .stream()
                .filter(member -> member.getSquibsPoints() > 0)
                .sorted(Comparator.comparing(MemberInfo::getSquibsPoints)
                                  .reversed())
                .collect(Collectors.toList());

        double squadronPoints = 0;
        squadronPoints += sortedByPoints.stream().limit(20)
                                        .mapToDouble(MemberInfo::getSquibsPoints)
                                        .sum();
        squadronPoints += sortedByPoints.stream().skip(20)
                                        .mapToDouble(m -> (m.getSquibsPoints() / 20.0))
                                        .sum();

        return squadronPoints;
    }

    public void setSessionWins(int wins) {
        sessionWins = wins;
        sendSessionWinLossResponse(true);
    }

    public void setSessionLosses(int losses) {
        sessionLosses = losses;
        sendSessionWinLossResponse(true);
    }

    public void watchPoints(List<MemberInfo> newMemberInfos) {
        log.debug("watchPoints - Watching points. Last we had: {}", lastSquadronPoints);

        if (sessionWins != 0 && sessionLosses != 0) {
            final int gmtHourNow = ZonedDateTime.now(ZoneId.of("GMT")).getHour();
            log.debug("gmtHourNow: {}", gmtHourNow);
            // SQB runs 14-22 GMT (EU) and 01-07 GMT (US)
            if (gmtHourNow == 23 || gmtHourNow == 8) {
                log.debug("Resetting session wins and losses");
                sessionLosses = 0;
                sessionWins = 0;
                sendSessionWinLossResponse(true);
            }
        }

        if (lastMemberInfos == null || lastSquadronPoints == null) {
            lastSquadronPoints = calculateSquadronPoints(newMemberInfos);
            lastMemberInfos = newMemberInfos;
            log.debug("watchPoints - First run. Saving data and skipping.");
            return;
        }

        final double squadronPointsDiff =
                calculateSquadronPoints(newMemberInfos) - lastSquadronPoints;

        if (squadronPointsDiff == 0.0) {
            log.debug("watchPoints - Points didn't change.");
        } else {
            log.debug("watchPoints - Points changed, diff is {}", squadronPointsDiff);
            log.debug("watchPoints - Old member info: {}", lastMemberInfos);
            log.debug("watchPoints - New member info: {}", newMemberInfos);
        }

        final SqbPointChanges changes = new SqbPointChanges();
        final StringBuilder response = new StringBuilder();
        int numberOfPlayersLeftOrRenamed = 0;

        for (MemberInfo lastMemberInfo : lastMemberInfos) {
            log.debug("watchPoints - Checking points for player: {}", lastMemberInfo.getName());
            final Optional<MemberInfo> newInfoOnMemberOptional = newMemberInfos
                    .stream()
                    .filter(info -> info.getName().equals(lastMemberInfo.getName()))
                    .findFirst();

            if (newInfoOnMemberOptional.isPresent()) {
                final MemberInfo newInfoOnMember = newInfoOnMemberOptional.get();
                final int memberDiff =
                        newInfoOnMember.getSquibsPoints() - lastMemberInfo.getSquibsPoints();

                if (memberDiff != 0) {
                    changes.addChange(new PointChange(lastMemberInfo, newInfoOnMember));

                    // TODO: Move logging to PointChange?
                    log.debug("watchPoints - SQBPointsLog - before: {}, now: {}, diff: {}, "
                                      + "player: {}",
                              lastMemberInfo.getSquibsPoints(),
                              newInfoOnMember.getSquibsPoints(),
                              memberDiff, newInfoOnMember.getName());
                }
            } else {
                log.debug("watchPoints - Player {} not in new data - member no longer with us or "
                                  + "changed name", lastMemberInfo.getName());

                final Optional<MemberInfo> playerWithSamePointsNotInLastUpdate = newMemberInfos
                        .stream()
                        .filter(newInfo -> lastMemberInfo.getSquibsPoints()
                                                         .equals(newInfo.getSquibsPoints()))
                        .filter(memberNotInLastUpdate(lastMemberInfos))
                        .findFirst();

                if (playerWithSamePointsNotInLastUpdate.isPresent()) {
                    final MemberInfo renamedPlayer = playerWithSamePointsNotInLastUpdate.get();
                    response.append(lastMemberInfo.getDiscordEscapedName())
                            .append(" has changed their in game name to ")
                            .append(renamedPlayer.getDiscordEscapedName())
                            .append(".");
                    numberOfPlayersLeftOrRenamed += 1;
                } else {
                    numberOfPlayersLeftOrRenamed += 1;

                    response.append(lastMemberInfo.getDiscordEscapedName())
                            .append(" is no longer in the squadron. They held ")
                            .append(lastMemberInfo.getSquibsPoints())
                            .append(" points.");
                }
            }
        }

        log.debug("watchPoints - numberOfChanges: {}", changes.getNumberOfChanges());

        if (changes.getNumberOfChanges() > 30 || numberOfPlayersLeftOrRenamed > 10
                || Math.abs(squadronPointsDiff) > 3000) {
            log.info("Triggering safeguard. numberOfChanges: {}, numberOfPlayersLeftOrRenamed: {}, "
                             + "squadronPointsDiff: {}",
                     changes.getNumberOfChanges(), numberOfPlayersLeftOrRenamed,
                     squadronPointsDiff);
            sendResponse("Either I got Gaijined or season reset?");
        } else {
            if (changes.getNumberOfChanges() > 0 || numberOfPlayersLeftOrRenamed > 0) {
                if (changes.getNumberOfWins() > 0) {
                    sessionWins += changes.getNumberOfWins();

                    for (int i = 0; i < changes.getNumberOfWins(); i++) {
                        if (i >= 1) {
                            response.append("\n");
                        }
                        response.append("WIN! We won a match.");
                    }
                }

                if (changes.getNumberOfLosses() > 0) {
                    sessionLosses += changes.getNumberOfLosses();

                    for (int i = 0; i < changes.getNumberOfLosses(); i++) {
                        if (i >= 1 || changes.getNumberOfWins() > 0) {
                            response.append("\n");
                        }
                        response.append("LOSS! We lost a match.");
                    }
                }

                response.append(" We ")
                        .append((squadronPointsDiff > 0)
                                        ? "gained "
                                        : "lost ")
                        .append((squadronPointsDiff == 0)
                                        ? "no"
                                        : String.format("%.1f", Math.abs(squadronPointsDiff)))
                        .append(" points.");

                log.debug("watchPoints - Sending response: {}", response.toString());
                sendResponse(response.toString());

                sendSessionWinLossResponse(false);
            }
        }

        lastSquadronPoints = calculateSquadronPoints(newMemberInfos);
        lastMemberInfos = newMemberInfos;
    }

    private void sendSessionWinLossResponse(boolean updatedValues) {
        final StringBuilder response = new StringBuilder();

        if (updatedValues) {
            response.append("Win/loss was manually updated. ");
        }

        response.append("Session total win/loss is ")
                .append(sessionWins)
                .append("/")
                .append(sessionLosses)
                .append(".");

        sendResponse(response.toString());
    }

    private Predicate<MemberInfo> memberNotInLastUpdate(List<MemberInfo> lastMemberInfos) {
        return newMember -> lastMemberInfos.stream()
                                           .noneMatch(last -> last.getName()
                                                                  .equals(newMember.getName()));
    }

    protected void sendResponse(String message) {
        // MessageHelper.sendMessage(client.getChannelByID(402693205818081280L), message);
        MessageHelper.sendMessage(client.getChannelByID(BOT_TEST_TEXT_ID), message);
    }

    private void sendErrorMessage() {
        MessageHelper.sendMessage(client.getChannelByID(BOT_TEST_TEXT_ID), ""
                + "Squibs points watcher ran into a problem and the timer was stopped. "
                + "You can try !watchpoints to restart it. If that doesn't work - yell at Teo.");
    }

    private void sendWarningMessage() {
        MessageHelper.sendMessage(client.getChannelByID(BOT_TEST_TEXT_ID),
                                  "Failed to look up points this time, trying again.");
    }

    private void sendRecoveredMessage() {
        MessageHelper.sendMessage(client.getChannelByID(BOT_TEST_TEXT_ID),
                                  "Looks like things are okay again.");
    }
}
