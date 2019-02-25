package taneltomson.discord.commands.listeners;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Slf4j
public class GiveawayListenerTest extends CommandTestBase {
    private static final String COMMAND_GIVEAWAY = "giveaway";
    private static final String COMMAND_ADD = "add";
    private static final String COMMAND_ADD_LIST = "add list";
    private static final String COMMAND_DELETE = "delete";
    private static final String COMMAND_INFO = "info";
    private static final String COMMAND_DRAW = "draw";
    private static final String COMMAND_CLEAR = "clear";

    private CommandListener giveawayListener;
    private Random randomMock = mock(Random.class);

    @Before
    public void setUp() {
        final IRole roleMock = mock(IRole.class);
        when(roleMock.getName()).thenReturn("Officer");

        setMessageAutherRoles(new ArrayList<>(Collections.singletonList(roleMock)));

        super.setUp();

        giveawayListener = new GiveawayListener(randomMock);
    }

    @Test
    public void testDoesNothingWhenWrongCommand() {
        verifyNothingDone("wrongCommand", COMMAND_INFO, messageInGeneralEvent);
        verifyNothingDone("wrongCommand", COMMAND_INFO, messageInBotTestEvent);
    }

    @Test
    public void testIncorrectInvocations() {
        verifyNothingDone("giveaway", "", messageInGeneralEvent);
        verifyNothingDone("giveaway", "", messageInBotTestEvent);
        verifyNothingDone("giveaway", "asdfasd", messageInGeneralEvent);
        verifyNothingDone("giveaway", "asgsdfs", messageInBotTestEvent);
    }

    @Test
    public void testInfoWhenNoParticipants() {
        verifyGiveawayHasNoParticipants();
    }

    @Test
    public void testInfoWithParticipants() {
        final List<String> participants = Arrays.asList("person1", "person 2", "person  3");
        Collections.sort(participants);
        addParticipantsToGiveaway(participants);

        verifyGiveawayHasTheseExactParticipants(participants);
    }

    @Test
    public void testAddParticipants() {
        final List<String> participants = Arrays.asList("person1", "person 2", "person  3");
        Collections.sort(participants);
        final List<String> participantsAdded = new ArrayList<>();

        participants.forEach(name -> {
            log.debug("Handling name: {}", name);

            participantsAdded.add(name);
            giveawayListener.handleCommand(COMMAND_GIVEAWAY, COMMAND_ADD + " " + name,
                                           messageInBotTestEvent);

            verifyMessageSent(botTestChannelMock, getParticipantsAddedResponse(name));
            verifyGiveawayHasTheseExactParticipants(participantsAdded);
        });
    }

    @Test
    public void testAddParticipantsList() {
        final List<String> participants = Arrays.asList("person1", "person 2", "person  3");
        Collections.sort(participants);

        giveawayListener.handleCommand(COMMAND_GIVEAWAY,
                                       COMMAND_ADD_LIST + "\n" + String.join("\n", participants),
                                       messageInBotTestEvent);

        verifyMessageSent(botTestChannelMock, getParticipantsListAddedResponse(participants.size()));
        verifyGiveawayHasTheseExactParticipants(participants);
    }

    @Test
    public void testDeleteParticipantThatDoesNotExist() {
        final List<String> participants = Arrays.asList("person1", "person 2", "person  3");
        addParticipantsToGiveaway(participants);

        // clear invocations from adding participants
        clearInvocations(botTestChannelMock);

        giveawayListener.handleCommand(COMMAND_GIVEAWAY, COMMAND_DELETE + " nonExistingPerson",
                                       messageInBotTestEvent);
        verifyMessageSent(botTestChannelMock, getDeleteErrorString("nonExistingPerson"));
    }

    @Test
    public void testDeleteParticipants() {
        final List<String> participants = new ArrayList<>(Arrays.asList("person1", "person 2",
                                                                        "person  3"));
        Collections.sort(participants);
        addParticipantsToGiveaway(participants);

        // clear invocations from adding participants
        clearInvocations(botTestChannelMock);

        giveawayListener.handleCommand(COMMAND_GIVEAWAY, COMMAND_DELETE + " person1",
                                       messageInBotTestEvent);
        verifyMessageSent(botTestChannelMock, getDeleteResponseString("person1"));

        participants.remove("person1");
        verifyGiveawayHasTheseExactParticipants(participants);
    }

    @Test
    public void testDrawWhenNoParticipants() {
        verifyGiveawayHasNoParticipants();

        giveawayListener.handleCommand(COMMAND_GIVEAWAY, COMMAND_DRAW, messageInGeneralEvent);
        verifyMessageSent(generalChannelMock, getDrawErrorString());
    }

    @Test
    public void testDrawInGeneral() {
        final List<String> participants = Arrays.asList("person1", "person 2", "person  3");
        Collections.sort(participants);
        addParticipantsToGiveaway(participants);

        final String winner = participants.get(2);
        when(randomMock.nextInt(3)).thenReturn(2);

        giveawayListener.handleCommand(COMMAND_GIVEAWAY, COMMAND_DRAW, messageInGeneralEvent);
        verifyMessageSent(generalChannelMock, getDrawResultString(winner));
        verifyMessageSent(botTestChannelMock,
                          getReminderToClearMessage(messageInGeneralEvent.getAuthor()));
    }

    @Test
    public void testDrawInBotTest() {
        final List<String> participants = Arrays.asList("person1", "person 2", "person  3");
        Collections.sort(participants);
        addParticipantsToGiveaway(participants);

        final String winner = participants.get(2);
        when(randomMock.nextInt(3)).thenReturn(2);

        giveawayListener.handleCommand(COMMAND_GIVEAWAY, COMMAND_DRAW, messageInBotTestEvent);
        verifyMessageSent(botTestChannelMock, getDrawResultString(winner));
        verifyMessageSent(botTestChannelMock,
                          getReminderToClearMessage(messageInBotTestEvent.getAuthor()));
    }

    @Test
    public void testClearWhenNoParticipants() {
        giveawayListener.handleCommand(COMMAND_GIVEAWAY, COMMAND_CLEAR, messageInBotTestEvent);
        verifyMessageSent(botTestChannelMock, getParticipantsClearedResponse());

        verifyGiveawayHasNoParticipants();
    }

    @Test
    public void testClear() {
        final List<String> participants = Arrays.asList("person1", "person 2", "person  3");
        addParticipantsToGiveaway(participants);

        giveawayListener.handleCommand(COMMAND_GIVEAWAY, COMMAND_CLEAR, messageInBotTestEvent);
        verifyMessageSent(botTestChannelMock, getParticipantsClearedResponse());

        verifyGiveawayHasNoParticipants();
    }

    @Test
    public void testOnlyInfoAndDrawCanBeUsedInGeneral() {
        verifyNothingDone("giveaway", "add", messageInGeneralEvent);
        verifyNothingDone("giveaway", "add someName1", messageInGeneralEvent);
        verifyNothingDone("giveaway", "delete", messageInGeneralEvent);
        verifyNothingDone("giveaway", "delete someName2", messageInGeneralEvent);
        verifyNothingDone("giveaway", "clear", messageInGeneralEvent);
    }

    @Test
    public void testBelowOfficersCannotUseGiveaway() {
        final IGuild guild = messageInGeneralEvent.getGuild();
        final IRole belowRequiredRoleMock = mock(IRole.class);
        final List<IRole> authorRoles = new ArrayList<>(Arrays.asList(belowRequiredRoleMock));
        when(messageInGeneralEvent.getAuthor().getRolesForGuild(guild))
                .thenReturn(authorRoles);

        verifyNothingDone("giveaway", "info", messageInGeneralEvent);
        verifyNothingDone("giveaway", "draw", messageInGeneralEvent);
    }

    private String getReminderToClearMessage(IUser author) {
        return author + ", please don't forget to clear the participants from the giveaway when "
                + "you're finished. I'm dumb and don't otherwise know to do it myself. That way "
                + "the next giveaway can start from a clean sheet."
                + "\n\n"
                + "You can do this with the command:"
                + "\n"
                + "!giveaway clear";
    }

    private void verifyNothingDone(String command, String arguments, MessageReceivedEvent event) {
        giveawayListener.handleCommand(command, arguments, event);
        verifyNoMessageSent(generalChannelMock);
        verifyNoMessageSent(botTestChannelMock);
    }

    private void verifyGiveawayHasNoParticipants() {
        verifyGiveawayHasTheseExactParticipants(new ArrayList<>());
    }

    private void verifyGiveawayHasTheseExactParticipants(List<String> participants) {
        giveawayListener.handleCommand(COMMAND_GIVEAWAY, COMMAND_INFO, messageInGeneralEvent);
        verifyMessageSent(generalChannelMock, getInfoString(participants));
        giveawayListener.handleCommand(COMMAND_GIVEAWAY, COMMAND_INFO, messageInBotTestEvent);
        verifyMessageSent(botTestChannelMock, getInfoString(participants));
    }

    private void addParticipantsToGiveaway(List<String> participants) {
        participants.forEach(name -> giveawayListener.handleCommand(
                COMMAND_GIVEAWAY, COMMAND_ADD + " " + name, messageInBotTestEvent));
    }

    private String getDrawResultString(String winner) {
        return String.format("The winner is:%n%s", winner);
    }

    private String getDrawErrorString() {
        return "Add participants first!";
    }

    private String getDeleteResponseString(String name) {
        return String.format("Deleted person %s.", name);
    }

    private String getDeleteErrorString(String name) {
        return String.format("No person %s in giveaway!", name);
    }

    private String getParticipantsAddedResponse(String name) {
        return String.format("Added participant %s.", name);
    }

    private String getParticipantsListAddedResponse(int size) {
        return String.format("Added %s participants.", size);
    }

    private String getParticipantsClearedResponse() {
        return "Participants cleared, ready to start a new giveaway.";
    }

    private String getInfoString(List<String> participants) {
        final StringBuilder sb = new StringBuilder();

        if (participants == null || participants.isEmpty()) {
            sb.append("No giveaway currently set up.");
        } else {
            sb.append("Giveaway set up with following participants:\n");
            participants.forEach(name -> sb.append(name).append("\n"));
            sb.append("Each person has equal chance of winning.");
        }

        return sb.toString();
    }
}