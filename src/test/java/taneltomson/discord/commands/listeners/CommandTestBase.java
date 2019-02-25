package taneltomson.discord.commands.listeners;


import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import taneltomson.discord.data.ChannelIds;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static taneltomson.discord.data.ChannelIds.BOT_TEST_TEXT_ID;
import static taneltomson.discord.data.ChannelIds.GENERAL_TEXT_ID;


public class CommandTestBase {
    protected MessageReceivedEvent messageInGeneralEvent;
    protected IChannel generalChannelMock;
    protected MessageReceivedEvent messageInBotTestEvent;
    protected IChannel botTestChannelMock;
    private ArrayList<IRole> authorRoles = new ArrayList<>();

    protected void setMessageAutherRoles(ArrayList<IRole> authorRoles) {
        this.authorRoles = authorRoles;
    }

    @Before
    public void setUp() {
        messageInGeneralEvent = mock(MessageReceivedEvent.class);
        generalChannelMock = mock(IChannel.class);
        when(generalChannelMock.getLongID()).thenReturn(GENERAL_TEXT_ID);
        when(generalChannelMock.getName()).thenReturn("GENERAL");
        when(messageInGeneralEvent.getChannel()).thenReturn(generalChannelMock);

        messageInBotTestEvent = mock(MessageReceivedEvent.class);
        botTestChannelMock = mock(IChannel.class);
        when(botTestChannelMock.getLongID()).thenReturn(BOT_TEST_TEXT_ID);
        when(botTestChannelMock.getName()).thenReturn("BOT TEST");
        when(messageInBotTestEvent.getChannel()).thenReturn(botTestChannelMock);

        final IUser authorUserMock = mock(IUser.class);
        when(messageInGeneralEvent.getAuthor()).thenReturn(authorUserMock);
        when(messageInBotTestEvent.getAuthor()).thenReturn(authorUserMock);

        final IDiscordClient clientMock = mock(IDiscordClient.class);
        when(messageInBotTestEvent.getClient()).thenReturn(clientMock);
        when(messageInGeneralEvent.getClient()).thenReturn(clientMock);

        when(clientMock.getChannelByID(ChannelIds.BOT_TEST_TEXT_ID)).thenReturn(botTestChannelMock);

        final IGuild guildMock = mock(IGuild.class);
        when(messageInGeneralEvent.getGuild()).thenReturn(guildMock);
        when(messageInBotTestEvent.getGuild()).thenReturn(guildMock);

        when(authorUserMock.getRolesForGuild(guildMock)).thenReturn(authorRoles);

        final IMessage messageMock = mock(IMessage.class);
        when(messageInBotTestEvent.getMessage()).thenReturn(messageMock);
        when(messageInGeneralEvent.getMessage()).thenReturn(messageMock);

        when(messageMock.getMentions()).thenReturn(new ArrayList<>());
        when(messageMock.getRoleMentions()).thenReturn(new ArrayList<>());
    }

    @After
    public void tearDown() {
    }

    protected void verifyMessageSent(IChannel channel, String message) {
        verify(channel, times(1)).sendMessage(message);
    }

    protected void verifyNoMessageSent(IChannel channel) {
        verify(channel, times(0)).sendMessage(anyString());
    }
}
