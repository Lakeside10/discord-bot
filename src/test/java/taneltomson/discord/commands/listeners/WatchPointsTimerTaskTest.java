package taneltomson.discord.commands.listeners;


import java.time.LocalDate;
import java.util.Arrays;
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
        assertResponseWasSent(deepCopyAndAddPointsToEight(10),
                              "WIN! We won a match. We gained 80.0 points.");
    }

    @Test
    public void testLosingAMatch() {
        assertResponseWasSent(deepCopyAndAddPointsToEight(-10),
                              "LOSS! We lost a match. We lost 80.0 points.");
    }

    @Test
    public void testLosingAMatchWithPlayersOnNoPoints() {
        assertResponseWasSent(deepCopyAndAddPointsTo(-10, 7),
                              "LOSS! We lost a match. We lost 70.0 points.");

        assertResponseWasSent(deepCopyAndAddPointsTo(-10, 1),
                              "LOSS! We lost a match. We lost 10.0 points.");
    }

    @Test
    public void testMultipleMatchesPlayed() {
        assertResponseWasSent(deepCopyAndAddPointsTo(20, 16),
                              "Multiple games were played. We gained 320.0 points.");
    }

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

        assertResponseWasSent(newMemberInfos,
                              memberWithPoints.getDiscordEscapedName()
                                      + " is no longer in the squadron. They held 100 points. We "
                                      + "lost 100.0 points.");
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
    private List<MemberInfo> deepCopyAndAddPointsToEight(int pointsToAdd) {
        return deepCopyAndAddPointsTo(pointsToAdd, 8);
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
    private List<MemberInfo> deepCopyAndAddPointsTo(int pointsToAdd, int numberOfPlayersToAddTo) {
        final List<MemberInfo> newMemberInfos = deepCopy(memberInfosBefore);

        final Supplier<Stream<MemberInfo>> supplier =
                () -> newMemberInfos.stream()
                                    .filter(i -> i.getSquibsPoints() >= -pointsToAdd)
                                    .limit(numberOfPlayersToAddTo);

        if (supplier.get().count() < numberOfPlayersToAddTo) {
            throw new RuntimeException("Tests set up incorrectly - not enough suitable members to "
                                               + "apply points change to. Needed " +
                                               numberOfPlayersToAddTo + " players.");
        }

        supplier.get()
                .forEach(i -> i.setSquibsPoints(i.getSquibsPoints() + pointsToAdd));

        return newMemberInfos;
    }

    private List<MemberInfo> createSquadronMemberInfos() {
        return Arrays.asList(createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
                             createMemberWithPoints(100),
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
