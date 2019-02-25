package taneltomson.discord.commands.listeners;


import java.util.List;

import javax.persistence.NoResultException;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import taneltomson.discord.TestConfiguration;
import taneltomson.discord.common.model.Command;
import taneltomson.discord.common.service.CustomCommandService;
import taneltomson.discord.common.service.DatabaseTestHelper;
import taneltomson.discord.data.ChannelIds;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static taneltomson.discord.commands.listeners.CustomCommandListener.CREATION_ERROR_RESPONSE;
import static taneltomson.discord.commands.listeners.CustomCommandListener.DELETION_ERROR_RESPONSE;
import static taneltomson.discord.commands.listeners.CustomCommandListener
        .DELETION_SUCCESS_RESPONSE;
import static taneltomson.discord.data.ChannelIds.GENERAL_TEXT_ID;


@Slf4j
public class CustomCommandListenerTest extends CommandTestBase {
    private static final String COMMAND_CREATE = "create";
    private static final String VALID_CREATE_ARGUMENTS = String.join(" ", "commandName",
                                                                     "Command value");

    private DatabaseTestHelper dbTestHelper = new DatabaseTestHelper();

    private CustomCommandService service;

    @Before
    public void setUp() {
        super.setUp();
        dbTestHelper.setUp();

        service = new CustomCommandService(TestConfiguration.getTestPUName());
    }

    @After
    public void tearDown() {
        super.tearDown();
        dbTestHelper.tearDown();

        service.close();
    }

    @Test
    public void testCreatesCommandOnBotTestRoom() {
        final CustomCommandListener ccl = new CustomCommandListener(service);

        assertCreatesCommandInBotTestRoom(ccl, "testCommand", "value is");
        assertCreatesCommandInBotTestRoom(ccl, "sqbBr", "The current SQB br is \n5.3");
    }

    @Test
    public void testCannotCreateCommandsInGeneral() {
        final CustomCommandListener ccl = new CustomCommandListener(service);

        ccl.handleCommand(COMMAND_CREATE, VALID_CREATE_ARGUMENTS, messageInGeneralEvent);

        assertThat(service.hasCommand("commandName"), is(false));
        verify(messageInGeneralEvent.getChannel(), times(0)).sendMessage(any(String.class));
    }

    @Test
    public void testAllowedChannelIds() {
        final CustomCommandListener ccl = new CustomCommandListener(null);
        final List<Long> allowedChannelIds = ccl.getAllowedChannelIds();

        assertThat(allowedChannelIds.size(), is(2));
        assertThat(allowedChannelIds.contains(ChannelIds.BOT_TEST_TEXT_ID), is(true));
        assertThat(allowedChannelIds.contains(GENERAL_TEXT_ID), is(true));
    }

    @Test
    public void testCanOnlyDeleteInBotTestRoom() {
        final CustomCommandListener ccl = new CustomCommandListener(service);

        ccl.handleCommand(COMMAND_CREATE, VALID_CREATE_ARGUMENTS, messageInGeneralEvent);

        new CustomCommandListener(service)
                .handleCommand("delete", String.join(" ", "name"), messageInGeneralEvent);
        verify(messageInGeneralEvent.getChannel(), times(0)).sendMessage(any(String.class));
    }

    @Test
    // TODO: Should return error message
    public void testArgumentValidationWhenCreatingCommand() {
        final CustomCommandListener ccl = new CustomCommandListener(service);

        ccl.handleCommand(COMMAND_CREATE, "", messageInBotTestEvent);
        try {
            service.findCommand("");
            Assert.fail();
        } catch (NoResultException e) {
            // pass
        }

        ccl.handleCommand(COMMAND_CREATE, "name", messageInBotTestEvent);
        try {
            assertThat(service.findCommand("name"), is(nullValue()));
            Assert.fail();
        } catch (NoResultException e) {
            // pass
        }
    }

    @Test
    public void testDeleteCommand() {
        final CustomCommandListener ccl = new CustomCommandListener(service);

        assertCreatesCommandInBotTestRoom(ccl, "testCommandDelete", "value is");

        assertThat(service.hasCommand("testcommanddelete"), is(true));
        new CustomCommandListener(service).handleCommand("delete",
                                                         String.join(" ", "testCommandDelete"),
                                                         messageInBotTestEvent);
        verify(messageInBotTestEvent.getChannel(), times(1)).sendMessage(DELETION_SUCCESS_RESPONSE);
        assertThat(service.hasCommand("testcommanddelete"), is(false));
    }

    @Test
    public void testDeletingNonExistingCommandGivesError() {
        assertThat(service.hasCommand("testcommandmissing"), is(false));
        new CustomCommandListener(service).handleCommand("delete",
                                                         String.join(" ", "testcommandmissing"),
                                                         messageInBotTestEvent);
        verify(messageInBotTestEvent.getChannel(), times(1)).sendMessage(DELETION_ERROR_RESPONSE);
    }

    @Test
    public void testAskingForCommand() {
        final CustomCommandListener ccl = new CustomCommandListener(service);

        assertCreatesCommandInBotTestRoom(ccl, "testCommandAsking", "value is something");

        ccl.handleCommand("testcommandasking", "",
                          messageInGeneralEvent);
        verify(messageInGeneralEvent.getChannel(), times(1)).sendMessage("value is something");
    }

    @Test
    public void testAskingForNonExistingCommand() {
        assertThat(service.hasCommand("nonExisting"), is(false));

        verifyNothingSent("nonExisting");
    }

    @Test
    public void testCreatingReservedCommand() {
        verifyCannotCreateCommandWithName("inspire");
        verifyCannotCreateCommandWithName("veg");
        verifyCannotCreateCommandWithName("addsqb");
        verifyCannotCreateCommandWithName("removesqb");
        verifyCannotCreateCommandWithName("create");
        verifyCannotCreateCommandWithName("delete");
        verifyCannotCreateCommandWithName("rank");
        verifyCannotCreateCommandWithName("top");
        verifyCannotCreateCommandWithName("points");
        verifyCannotCreateCommandWithName("joined");
        verifyCannotCreateCommandWithName("help");
    }

    @Test
    public void testHelpCommand() {
        final CustomCommandListener ccl = new CustomCommandListener(service);

        ccl.handleCommand("help", "", messageInGeneralEvent);
    }

    private void verifyCannotCreateCommandWithName(String commandName) {
        final CustomCommandListener ccl = new CustomCommandListener(service);
        assertThat(service.hasCommand(commandName), is(false));

        ccl.handleCommand("create", String.join(" ", commandName, "some value"),
                          messageInBotTestEvent);
        assertThat("Should not have created command: " + commandName,
                   service.hasCommand(commandName), is(false));
    }

    private void verifyNothingSent(String commandName) {
        new CustomCommandListener(service).handleCommand(commandName, "", messageInGeneralEvent);

        verify(messageInGeneralEvent.getChannel(), times(0)).sendMessage(any(String.class));
    }

    private void assertCreatesCommandInBotTestRoom(CustomCommandListener ccl,
                                                   String commandName,
                                                   String commandValue) {

        ccl.handleCommand(COMMAND_CREATE, String.join(" ", commandName, commandValue),
                          messageInBotTestEvent);

        final Command command = service.findCommand(commandName.toLowerCase());
        assertThat(command, is(notNullValue()));
        assertThat(command.getCallKey(), is(commandName.toLowerCase()));
        assertThat(command.getValue(), is(commandValue));
    }
}