package taneltomson.discord.commands.listeners;


import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sx.blah.discord.api.IDiscordClient;
import taneltomson.discord.commands.listeners.util.WatchPointsTimerTask;
import taneltomson.discord.util.web.data.MemberInfo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;


public class WatchPointsTimerTaskTest {
    private static final LocalDate aDate = LocalDate.of(2018, 1, 1);

    private TestableWatchPointsTimerTask watcher;
    private List<MemberInfo> memberInfosBefore;

    @Before
    public void setUp() {
        final IDiscordClient clientMock = mock(IDiscordClient.class);
        watcher = new TestableWatchPointsTimerTask(clientMock);
        memberInfosBefore = createSquadronMemberInfos();

        // First run - save previous state and skip response
        assertNoMessageWouldBeSent(watcher, memberInfosBefore);
    }

    @Test
    public void testWinningAMatch() {
        assertResponseWasSent(deepCopyAndAddPointsToEightTopRated(10),
                              "WIN! We won a match. We gained 80.0 points.");
    }

    @Test
    public void testLosingAMatch() {
        assertResponseWasSent(deepCopyAndAddPointsToEightTopRated(-10),
                              "LOSS! We lost a match. We lost 80.0 points.");
    }

    @Test
    public void testLosingAMatchWithPlayersOnNoPoints() {
        assertResponseWasSent(deepCopyAndAddPointsToHighestRated(-10, 7),
                              "LOSS! We lost a match. We lost 70.0 points.");

        assertResponseWasSent(deepCopyAndAddPointsToHighestRated(-10, 1),
                              "LOSS! We lost a match. We lost 10.0 points.");
    }

    @Test
    public void testTwoMatchesWon() {
        assertResponseWasSent(deepCopyAndAddPointsToHighestRated(20, 16), ""
                + "WIN! We won a match.\n"
                + "WIN! We won a match. We gained 320.0 points.");
    }

    @Test
    public void testThreeMatchesWon() {
        assertResponseWasSentAndStartedWith(deepCopyAndAddPointsToHighestRated(20, 24), ""
                + "WIN! We won a match.\n"
                + "WIN! We won a match.\n"
                + "WIN! We won a match. We gained ");
    }

    @Test
    public void testTwoMatchesLost() {
        assertResponseWasSentAndStartedWith(deepCopyAndAddPointsToHighestRated(-20, 16), ""
                + "LOSS! We lost a match.\n"
                + "LOSS! We lost a match. We lost ");
    }

    @Test
    public void testThreeMatchesLost() {
        assertResponseWasSentAndStartedWith(deepCopyAndAddPointsToHighestRated(-20, 24), ""
                + "LOSS! We lost a match.\n"
                + "LOSS! We lost a match.\n"
                + "LOSS! We lost a match. We lost ");
    }

    @Test
    public void testOneWonOneLost() {
        final List<MemberInfo> newMemberInfos = deepCopy(memberInfosBefore);
        addPointsToHighestRated(-10, 8, newMemberInfos);
        addPointsToHighestRated(10, 8, newMemberInfos);

        assertResponseWasSentAndStartedWith(newMemberInfos, ""
                + "WIN! We won a match.\n"
                + "LOSS! We lost a match. We lost ");
    }

    @Test
    public void testOneWonTwoLost() {
        final List<MemberInfo> newMemberInfos = deepCopy(memberInfosBefore)
                .stream()
                .sorted(Comparator.comparing(MemberInfo::getSquibsPoints).reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < 18; i++) {
            final MemberInfo memberInfo = newMemberInfos.get(i);

            if (i < 8) {
                // Add points to 8 to simulate win
                memberInfo.setSquibsPoints(memberInfo.getSquibsPoints() + 10);
            } else {
                // Remove points from 18 - 8 = 10 to simulate 2 losses
                memberInfo.setSquibsPoints(memberInfo.getSquibsPoints() - 20);
                assertThat("Can't have negative points",
                           memberInfo.getSquibsPoints() > 0, is(true));
            }
        }

        assertResponseWasSentAndStartedWith(newMemberInfos, ""
                + "WIN! We won a match.\n"
                + "LOSS! We lost a match.\n"
                + "LOSS! We lost a match. We lost ");
    }

    @Test
    public void testTwoWonOneLost() {
        final List<MemberInfo> newMemberInfos = deepCopy(memberInfosBefore)
                .stream()
                .sorted(Comparator.comparing(MemberInfo::getSquibsPoints).reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < 21; i++) {
            final MemberInfo memberInfo = newMemberInfos.get(i);

            if (i < 5) {
                // Remove points from 5 to simulate loss
                memberInfo.setSquibsPoints(memberInfo.getSquibsPoints() - 20);
                assertThat("Can't have negative points",
                           memberInfo.getSquibsPoints() > 0, is(true));
            } else {
                // Add points to 21 - 5 = 16 to simulate 2 wins
                memberInfo.setSquibsPoints(memberInfo.getSquibsPoints() + 20);
            }
        }

        assertResponseWasSentAndStartedWith(newMemberInfos, ""
                + "WIN! We won a match.\n"
                + "WIN! We won a match.\n"
                + "LOSS! We lost a match. We gained ");
    }

    @Test
    public void testTwoMatchesLostWithSomePlayersOnZeroPoints() {
        assertResponseWasSentAndStartedWith(deepCopyAndAddPointsToHighestRated(-20, 9), ""
                + "LOSS! We lost a match.\n"
                + "LOSS! We lost a match. We lost ");

        assertResponseWasSentAndStartedWith(deepCopyAndAddPointsToHighestRated(-20, 15), ""
                + "LOSS! We lost a match.\n"
                + "LOSS! We lost a match. We lost ");
    }

    // TODO: Test players leaving/renaming at the same time as match was played?

    // TODO: Test combos of multiple won/lost

    @Test
    public void testNewPlayerJoiningDoesNotBreakThings() {
        final List<MemberInfo> newMemberInfos = deepCopy(memberInfosBefore);
        newMemberInfos.add(createMemberWithPoints(0));

        assertNoMessageWouldBeSent(watcher, newMemberInfos);
    }

    @Test
    public void testPlayerWithNoPointsLeaving() {
        final List<MemberInfo> newMemberInfos = deepCopy(memberInfosBefore);

        final MemberInfo noPointsMember = getMemberWithPointsAmount(newMemberInfos, 0);
        newMemberInfos.remove(noPointsMember);

        assertResponseWasSent(newMemberInfos,
                              noPointsMember.getDiscordEscapedName()
                                      + " is no longer in the squadron. They held 0 points. We "
                                      + "lost no points.");
    }

    @Test
    public void testPlayerWithPointsLeaving() {
        final List<MemberInfo> newMemberInfos = deepCopy(memberInfosBefore);

        final MemberInfo memberWithPoints = getMemberWithPointsAmount(newMemberInfos, 100);
        newMemberInfos.remove(memberWithPoints);

        assertResponseWasSentAndStartedWith(newMemberInfos, ""
                + memberWithPoints.getDiscordEscapedName() + " is no longer in the squadron. "
                + "They held 100 points. We lost ");
    }

    @Test
    public void testPlayerChangingName() {
        final List<MemberInfo> newMemberInfos = deepCopy(memberInfosBefore);
        final MemberInfo memberWithPoints = getMemberWithPointsAmount(newMemberInfos, 100);

        final String oldName = memberWithPoints.getDiscordEscapedName();
        memberWithPoints.setName("New name");

        // TODO/XXX: No need to report that points didn't change
        assertResponseWasSent(newMemberInfos,
                              oldName + " has changed their in game name to New name. We lost no "
                                      + "points.");

        // TODO/XXX: Also a case when their points change?
    }

    private MemberInfo getMemberWithPointsAmount(List<MemberInfo> list, int points) {
        return list.stream()
                   .filter(i -> i.getSquibsPoints() == points)
                   .findFirst()
                   .orElseThrow(() -> new RuntimeException("Tests incorrectly set up - expected a "
                                                                   + "player with " + points + " "
                                                                   + "points."));
    }

    /**
     * Creates a deep copy of previous member infos and adds (or substracts) points from 8
     * members. Deep copy is made to avoid changing already saved {@link MemberInfo} objects in
     * {@link SquibsPointsWatcher}.
     *
     * @param pointsToAdd points to add (negative to subtract)
     * @return
     */
    private List<MemberInfo> deepCopyAndAddPointsToEightTopRated(int pointsToAdd) {
        return deepCopyAndAddPointsToHighestRated(pointsToAdd, 8);
    }

    /**
     * Creates a deep copy of previous member infos and adds (or substracts) points from
     * members. Deep copy is made to avoid changing already saved {@link MemberInfo} objects in
     * {@link SquibsPointsWatcher}.
     *
     * @param pointsToAdd            points to add (negative to subtract)
     * @param numberOfPlayersToAddTo number of players whose points to alter
     * @return
     */
    private List<MemberInfo> deepCopyAndAddPointsToHighestRated(int pointsToAdd,
                                                                int numberOfPlayersToAddTo) {
        final List<MemberInfo> newMemberInfos = deepCopy(memberInfosBefore);

        return addPointsToHighestRated(pointsToAdd, numberOfPlayersToAddTo, newMemberInfos);
    }

    private List<MemberInfo> addPointsToHighestRated(int pointsToAdd, int numberOfPlayersToAddTo,
                                                     List<MemberInfo> memberInfos) {
        final Supplier<Stream<MemberInfo>> supplier =
                () -> memberInfos.stream()
                                 .filter(i -> i.getSquibsPoints() >= -pointsToAdd)
                                 .sorted(Comparator.comparingInt(MemberInfo::getSquibsPoints)
                                                   .reversed())
                                 .limit(numberOfPlayersToAddTo);

        if (supplier.get().count() < numberOfPlayersToAddTo) {
            throw new RuntimeException("Tests set up incorrectly - not enough suitable members to "
                                               + "apply points change to. Needed " +
                                               numberOfPlayersToAddTo + " players. Had " +
                                               supplier.get().count() + " players.");
        }

        supplier.get()
                .forEach(i -> i.setSquibsPoints(i.getSquibsPoints() + pointsToAdd));

        return memberInfos;
    }

    private List<MemberInfo> createSquadronMemberInfos() {
        return Arrays.asList(createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(90),
                             createMemberWithPoints(90),
                             createMemberWithPoints(90),
                             createMemberWithPoints(90),
                             createMemberWithPoints(90),
                             createMemberWithPoints(90),
                             createMemberWithPoints(90),
                             createMemberWithPoints(90),
                             createMemberWithPoints(0),
                             createMemberWithPoints(0),
                             createMemberWithPoints(0),
                             createMemberWithPoints(0),
                             createMemberWithPoints(0),
                             createMemberWithPoints(0),
                             createMemberWithPoints(0),
                             createMemberWithPoints(0),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100));
    }

    private void assertResponseWasSent(List<MemberInfo> memberInfosAfterWin,
                                       String expectedResponse) {
        try {
            watcher.watchPoints(memberInfosAfterWin);
            Assert.fail("Expected message '" + expectedResponse + "' to be sent.");
        } catch (TestableWatchPointsTimerTask.ResponseSentException e) {
            assertThat(e.getMessageSent(), is(expectedResponse));
        }
    }

    private void assertResponseWasSentAndStartedWith(List<MemberInfo> memberInfosAfterWin,
                                                     String expectedResponseStart) {
        try {
            watcher.watchPoints(memberInfosAfterWin);
            Assert.fail("Expected message starting "
                                + "with '" + expectedResponseStart + "' to be sent.");
        } catch (TestableWatchPointsTimerTask.ResponseSentException e) {
            assertThat(e.getMessageSent(), startsWith(expectedResponseStart));
        }
    }

    private List<MemberInfo> deepCopy(List<MemberInfo> memberInfos) {
        return memberInfos.stream()
                          .map(i -> new MemberInfo(i.getName(), i.getSquibsPoints(),
                                                   i.getRole(), i.getJoinDate()))
                          .collect(Collectors.toList());
    }

    private void assertNoMessageWouldBeSent(WatchPointsTimerTask watcher,
                                            List<MemberInfo> memberInfos) {
        try {
            watcher.watchPoints(memberInfos);
        } catch (TestableWatchPointsTimerTask.ResponseSentException e) {
            Assert.fail("Expected no message to be sent, instead got: " + e.getMessageSent());
        }
    }

    private MemberInfo createMemberWithPoints(int points) {
        return new MemberInfo("name" + UUID.randomUUID().toString(), points, "Private", aDate);
    }
}
