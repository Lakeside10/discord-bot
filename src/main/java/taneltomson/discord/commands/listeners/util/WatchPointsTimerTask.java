package taneltomson.discord.commands.listeners.util;


import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TimerTask;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.IDiscordClient;
import taneltomson.discord.util.web.WTWebsiteScraper;
import taneltomson.discord.util.web.data.MemberInfo;

import static taneltomson.discord.data.ChannelIds.BOT_TEST_TEXT_ID;


@Slf4j
public class WatchPointsTimerTask extends TimerTask {
    private static int FAILURES_IN_A_ROW = 0;
    private static boolean PREVIOUS_FAILED = false;

    private final WTWebsiteScraper scraper = new WTWebsiteScraper();
    private final IDiscordClient client;
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

    public void watchPoints(List<MemberInfo> newMemberInfos) {
        log.debug("watchPoints - Watching points. Last we had: {}", lastSquadronPoints);

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

        final StringBuilder response = new StringBuilder();
        int numberOfChanges = 0;
        int numberOfPlayersLeft = 0;

        for (MemberInfo lastMemberInfo : lastMemberInfos) {
            log.debug("watchPoints - Checking points for player: {}", lastMemberInfo.getName());
            final Optional<MemberInfo> newInfoOnMemberOptional = newMemberInfos
                    .stream()
                    .filter(info -> info.getName().equals(lastMemberInfo.getName()))
                    .findFirst();

            if (newInfoOnMemberOptional.isPresent()) {
                final MemberInfo newInfoOnMember = newInfoOnMemberOptional.get();
                final int memberDiff =
                        lastMemberInfo.getSquibsPoints() - newInfoOnMember.getSquibsPoints();

                if (memberDiff != 0) {
                    log.debug("watchPoints - SQBPointsLog - before: {}, now: {}, diff: {}, "
                                      + "player: {}",
                              lastMemberInfo.getSquibsPoints(),
                              newInfoOnMember.getSquibsPoints(),
                              memberDiff, newInfoOnMember.getName());
                    numberOfChanges += 1;
                }
            } else {
                log.debug("watchPoints - Player {} not in new data - member no longer with us.",
                          lastMemberInfo.getName());
                numberOfPlayersLeft += 1;

                response.append(lastMemberInfo.getName())
                        .append(" is no longer in the squadron. They held ")
                        .append(lastMemberInfo.getSquibsPoints())
                        .append(" points.");
            }
        }

        log.debug("watchPoints - numberOfChanges: {}", numberOfChanges);

        if (numberOfChanges > 30 || numberOfPlayersLeft > 5
                || Math.abs(squadronPointsDiff) > 3000) {
            log.info("Triggering safeguard. numberOfChanges: {}, numberOfPlayersLeft: {}, "
                             + "squadronPointsDiff: {}",
                     numberOfChanges, numberOfPlayersLeft, squadronPointsDiff);
            sendResponse("Either I got Gaijined or season reset?");
        } else {
            if (numberOfChanges > 0 || numberOfPlayersLeft > 0) { // Something happened
                if (numberOfChanges > 0 && numberOfChanges <= 8) { // 1 match played
                    if (squadronPointsDiff > 0) {
                        response.append("WIN! We won a match.");
                    } else {
                        response.append("LOSS! We lost a match.");
                    }
                } else if (numberOfChanges > 8) { // More than one match was played
                    response.append("Multiple games were played.");
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
            }
        }

        lastSquadronPoints = calculateSquadronPoints(newMemberInfos);
        lastMemberInfos = newMemberInfos;
    }

    protected void sendResponse(String message) {
        client.getChannelByID(402693205818081280L).sendMessage(message);
        client.getChannelByID(BOT_TEST_TEXT_ID).sendMessage(message);
    }

    private void sendErrorMessage() {
        client.getChannelByID(BOT_TEST_TEXT_ID)
              .sendMessage("Squibs points watcher ran into a problem and the timer was stopped. "
                                   + "You can try !watchpoints to restart it. If that doesn't "
                                   + "work - yell at Teo.");
    }

    private void sendWarningMessage() {
        client.getChannelByID(BOT_TEST_TEXT_ID)
              .sendMessage("Failed to look up points this time, trying again.");
    }

    private void sendRecoveredMessage() {
        client.getChannelByID(BOT_TEST_TEXT_ID)
              .sendMessage("Looks like things are okay again.");
    }
}
