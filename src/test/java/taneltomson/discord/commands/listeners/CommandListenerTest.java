package taneltomson.discord.commands.listeners;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import taneltomson.discord.commands.listeners.CommandListener;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Slf4j
public class CommandListenerTest {
    private final static String VALID_COMMAND = "!valid command";
    private final static long VALID_CHANNEL_ID = 100L;

    @Test
    public void testWhenNoAllowedChannelsDoesNotHandle() throws Exception {
        final MessageReceivedEvent mockEvent = getMockMessageEventWith(VALID_CHANNEL_ID,
                                                                       VALID_COMMAND);
        final List<Long> allowedChannelIds = new ArrayList<>();

        assertDidNotHandleCommand(mockEvent, allowedChannelIds);
    }

    @Test
    public void testWhenNotInAllowedChannelsDoesNotHandle() throws Exception {
        final MessageReceivedEvent mockEvent = getMockMessageEventWith(100L, VALID_COMMAND);
        final List<Long> allowedChannelIds = Arrays.asList(50L, 150L, 300L);

        assertDidNotHandleCommand(mockEvent, allowedChannelIds);
    }

    @Test
    public void testWhenInAllowedChannelHandles() throws Exception {
        final MessageReceivedEvent mockEvent = getMockMessageEventWith(100L, VALID_COMMAND);
        final List<Long> allowedChannelIds = Collections.singletonList(100L);

        assertDidHandleEvent(mockEvent, allowedChannelIds);
    }

    @Test
    public void testWhenNotACommandDoesNotHandle() throws Exception {
        final MessageReceivedEvent mockEvent = getMockMessageEventWith(VALID_CHANNEL_ID,
                                                                       "not a command");
        final List<Long> allowedChannelIds = Collections.singletonList(VALID_CHANNEL_ID);

        assertDidNotHandleCommand(mockEvent, allowedChannelIds);
    }

    @Test
    public void testWhenEmptyCommandMarkDoesNotHandle() throws Exception {
        MessageReceivedEvent mockEvent = getMockMessageEventWith(VALID_CHANNEL_ID, "!");
        List<Long> allowedChannelIds = Collections.singletonList(VALID_CHANNEL_ID);
        assertDidNotHandleCommand(mockEvent, allowedChannelIds);

        mockEvent = getMockMessageEventWith(VALID_CHANNEL_ID, "! command");
        allowedChannelIds = Collections.singletonList(VALID_CHANNEL_ID);
        assertDidNotHandleCommand(mockEvent, allowedChannelIds);
    }

    @Test
    public void testHandlesWithCorrectValues() throws Exception {
        assertHandledWithCommandNameAndArguments(VALID_CHANNEL_ID, "!command",
                                                 "command", "");
        assertHandledWithCommandNameAndArguments(VALID_CHANNEL_ID, "!command arg1",
                                                 "command", "arg1");
        assertHandledWithCommandNameAndArguments(VALID_CHANNEL_ID, "!command arg1 arg2",
                                                 "command", "arg1 arg2");

    }

    private MessageReceivedEvent getMockMessageEventWith(long messageChannelId,
                                                         String messageContent) {
        final MessageReceivedEvent mockEvent = mock(MessageReceivedEvent.class);
        final IChannel mockChannel = mock(IChannel.class);
        final IMessage mockMessage = mock(IMessage.class);

        when(mockEvent.getChannel()).thenReturn(mockChannel);
        when(mockChannel.getLongID()).thenReturn(messageChannelId);

        when(mockEvent.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getContent()).thenReturn(messageContent);

        return mockEvent;
    }

    private void assertDidNotHandleCommand(MessageReceivedEvent mockEvent,
                                           List<Long> allowedChannelIds) {
        try {
            new TestCommandListener(allowedChannelIds).onMessageReceivedEvent(mockEvent);
        } catch (HandleCommandCalledException e) {
            Assert.fail("Should not have handled command");
        }
    }

    private void assertDidHandleEvent(MessageReceivedEvent mockEvent,
                                      List<Long> allowedChannelIds) {
        try {
            new TestCommandListener(allowedChannelIds).onMessageReceivedEvent(mockEvent);
            Assert.fail("Should have handled command");
        } catch (HandleCommandCalledException e) {
            // Expected, test passed
        }
    }

    private void assertHandledWithCommandNameAndArguments(long channelId, String messageContent,
                                                          String command, String arguments) {
        final MessageReceivedEvent mockEvent = getMockMessageEventWith(channelId, messageContent);
        final List<Long> allowedChannelIds = Collections.singletonList(channelId);

        try {
            new TestCommandListener(allowedChannelIds).onMessageReceivedEvent(mockEvent);
            Assert.fail("Should have handled command");
        } catch (HandleCommandCalledException e) {
            assertThat("Handled with incorrect command name", e.getCommand(), is(command));
            assertThat("Handled with incorrect arguments", e.getArguments(), is(arguments));
        }
    }


    @RequiredArgsConstructor
    private class TestCommandListener extends CommandListener {
        private final List<Long> allowedChannelIds;

        @Override
        protected List<Long> getAllowedChannelIds() {
            return allowedChannelIds;
        }

        @Override
        protected void handleCommand(String command, String arguments,
                                     MessageReceivedEvent event) {
            throw new HandleCommandCalledException(command, arguments, event);
        }
    }


    @RequiredArgsConstructor
    @Getter
    private class HandleCommandCalledException extends RuntimeException {
        private final String command;
        private final String arguments;
        private final MessageReceivedEvent event;
    }
}